//! Background video (README "Background video"), without a browser.
//!
//! The clips in the videos folder play one after another in file-name order (numbers compared as
//! numbers: bg-2 before bg-10), then round again from the first, forever, with no sound. FADE
//! seconds before a clip ends the next one starts and the ending clip fades out over what it has
//! left, so one clip dissolves into the next and none is cut short. A clip that cannot be read is
//! skipped until the next start.
//!
//! Decoding runs on its own thread: MP4 files are read with the `mp4` crate and their H.264
//! video decoded with OpenH264 (built into the launcher, no system FFmpeg). The thread publishes
//! the picture to show as Skia images; the window only draws them.

use std::collections::VecDeque;
use std::fs::File;
use std::io::BufReader;
use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};

use openh264::OpenH264API;
use openh264::decoder::{Decoder, DecoderConfig, Flush};
use openh264::formats::YUVSource;
use skia_safe::{AlphaType, ColorType, Data, Image, ImageInfo, images};

/// Seconds one clip takes to dissolve into the next; 0 = plain cut.
pub const FADE: f64 = 1.5;

/// Frames decoded ahead per clip: single frames of the larger clips take up to ~70 ms to decode
/// (two frames' time) although they average ~17 ms. 6 × 720p RGBA ≈ 22 MB.
const AHEAD: usize = 6;

/// What the window should draw now.
#[derive(Default, Clone)]
pub struct Picture {
    /// The running clip (the newer one while two overlap).
    pub front: Option<Image>,
    /// The ending clip during a dissolve, drawn over `front` at this opacity.
    pub fading: Option<(Image, f32)>,
    /// Bumped whenever the picture changes.
    pub generation: u64,
    /// Signalled on every bump, so the window redraws when a frame is due instead of polling.
    pub changed: Arc<tokio::sync::Notify>,
}

pub type SharedPicture = Arc<Mutex<Picture>>;

/// Every .mp4 in `dir`, in play order. (WebM is not supported by this player.)
pub fn list_videos(dir: &Path) -> Vec<PathBuf> {
    let Ok(entries) = std::fs::read_dir(dir) else { return Vec::new() };
    let mut files: Vec<PathBuf> = entries
        .flatten()
        .map(|e| e.path())
        .filter(|p| p.is_file() && p.extension().is_some_and(|e| e.eq_ignore_ascii_case("mp4")))
        .collect();
    files.sort_by(|a, b| natural_cmp(&a.file_name().unwrap_or_default().to_string_lossy(), &b.file_name().unwrap_or_default().to_string_lossy()));
    files
}

/// File-name order with runs of digits compared as numbers, letters without case.
pub fn natural_cmp(a: &str, b: &str) -> std::cmp::Ordering {
    use std::cmp::Ordering;
    let (mut a, mut b) = (a.chars().peekable(), b.chars().peekable());
    loop {
        match (a.peek().copied(), b.peek().copied()) {
            (None, None) => return Ordering::Equal,
            (None, Some(_)) => return Ordering::Less,
            (Some(_), None) => return Ordering::Greater,
            (Some(x), Some(y)) if x.is_ascii_digit() && y.is_ascii_digit() => {
                let take = |it: &mut std::iter::Peekable<std::str::Chars>| {
                    let mut digits = String::new();
                    while let Some(c) = it.peek().copied().filter(char::is_ascii_digit) {
                        digits.push(c);
                        it.next();
                    }
                    digits
                };
                let (da, db) = (take(&mut a), take(&mut b));
                let (ta, tb) = (da.trim_start_matches('0'), db.trim_start_matches('0'));
                let order = ta.len().cmp(&tb.len()).then_with(|| ta.cmp(tb)).then_with(|| da.len().cmp(&db.len()));
                if order != Ordering::Equal {
                    return order;
                }
            }
            (Some(x), Some(y)) => {
                let order = x.to_lowercase().cmp(y.to_lowercase());
                if order != Ordering::Equal {
                    return order;
                }
                a.next();
                b.next();
            }
        }
    }
}

struct Clip {
    reader: mp4::Mp4Reader<BufReader<File>>,
    track: u32,
    samples: u32,
    next_sample: u32,
    length_size: usize,
    decoder: Decoder,
    /// SPS and PPS in Annex B form, fed before the first sample.
    header: Option<Vec<u8>>,
    flushed: VecDeque<Image>,
    flushed_done: bool,
    duration: f64,
    frame_time: f64,
    /// Frames handed out so far; frame k is due at k * frame_time.
    shown: i64,
    last: Option<Image>,
    /// The next frames, decoded while waiting for them to be due.
    ahead: VecDeque<Image>,
    ended: bool,
    start: Instant,
}

fn annex_b(nal: &[u8], out: &mut Vec<u8>) {
    out.extend_from_slice(&[0, 0, 0, 1]);
    out.extend_from_slice(nal);
}

impl Clip {
    fn open(path: &Path, start: Instant) -> Result<Self, String> {
        let file = File::open(path).map_err(|e| e.to_string())?;
        let size = file.metadata().map_err(|e| e.to_string())?.len();
        let reader = mp4::Mp4Reader::read_header(BufReader::new(file), size).map_err(|e| e.to_string())?;
        let (track, length_size, header, samples, duration) = {
            let track = reader
                .tracks()
                .values()
                .find(|t| matches!(t.media_type(), Ok(mp4::MediaType::H264)))
                .ok_or("no H.264 video in the file")?;
            let mut header = Vec::new();
            annex_b(track.sequence_parameter_set().map_err(|e| e.to_string())?, &mut header);
            annex_b(track.picture_parameter_set().map_err(|e| e.to_string())?, &mut header);
            let length_size = track
                .trak
                .mdia
                .minf
                .stbl
                .stsd
                .avc1
                .as_ref()
                .map_or(4, |avc1| usize::from(avc1.avcc.length_size_minus_one & 3) + 1);
            (track.track_id(), length_size, header, track.sample_count(), track.duration().as_secs_f64())
        };
        if samples == 0 || duration <= 0. {
            return Err("empty video".into());
        }
        Ok(Self {
            reader,
            track,
            samples,
            next_sample: 1,
            length_size,
            // No flush after each decode: the crate's default forces a frame out per sample, which
            // with B-frames hands them over in decode order and breaks the decoder after a few
            // frames (dsOutOfMemory); without it frames come out reordered, in display order.
            decoder: Decoder::with_api_config(OpenH264API::from_source(), DecoderConfig::new().flush_after_decode(Flush::NoFlush))
                .map_err(|e| e.to_string())?,
            header: Some(header),
            flushed: VecDeque::new(),
            flushed_done: false,
            duration,
            frame_time: duration / f64::from(samples),
            shown: -1,
            last: None,
            ahead: VecDeque::new(),
            ended: false,
            start,
        })
    }

    fn elapsed(&self, now: Instant) -> f64 {
        now.saturating_duration_since(self.start).as_secs_f64()
    }

    /// The next frame in display order, or None at the end of the clip.
    fn decode_next(&mut self, rgba: &mut Vec<u8>) -> Option<Image> {
        while self.next_sample <= self.samples {
            let sample = self.reader.read_sample(self.track, self.next_sample).ok().flatten();
            self.next_sample += 1;
            let Some(sample) = sample else { continue };
            let mut packet = self.header.take().unwrap_or_default();
            let mut data: &[u8] = &sample.bytes;
            while data.len() > self.length_size {
                let len = data[..self.length_size].iter().fold(0usize, |acc, &b| (acc << 8) | usize::from(b));
                let end = (self.length_size + len).min(data.len());
                annex_b(&data[self.length_size..end], &mut packet);
                data = &data[end..];
            }
            if let Ok(Some(yuv)) = self.decoder.decode(&packet) {
                return convert(&yuv, rgba);
            }
        }
        if !self.flushed_done {
            self.flushed_done = true;
            if let Ok(frames) = self.decoder.flush_remaining() {
                for yuv in &frames {
                    if let Some(image) = convert(yuv, rgba) {
                        self.flushed.push_back(image);
                    }
                }
            }
        }
        self.flushed.pop_front()
    }

    /// The frame due `t` seconds into the clip (frames that are late are skipped).
    fn frame_at(&mut self, t: f64, rgba: &mut Vec<u8>) -> Option<Image> {
        if self.shown < 0 && !self.ended {
            // The first frame takes a while (decoder start, key frame): the clip's clock starts
            // once it is ready, so the frames after it are not all late at once.
            self.last = self.ahead.pop_front().or_else(|| self.decode_next(rgba));
            self.ended = self.last.is_none();
            self.shown = 0;
            self.start = Instant::now();
            return self.last.clone();
        }
        let due = (t / self.frame_time).floor() as i64;
        while self.shown < due && !self.ended {
            match self.ahead.pop_front().or_else(|| self.decode_next(rgba)) {
                Some(image) => {
                    self.last = Some(image);
                    self.shown += 1;
                }
                None => self.ended = true,
            }
        }
        self.last.clone()
    }

    /// Decodes frames ahead until `until` (at least one if none is ready), so a frame is ready
    /// the moment it is due even when some take longer than a frame's time to decode.
    fn prefetch(&mut self, until: Instant, rgba: &mut Vec<u8>) {
        while self.ahead.len() < AHEAD && !self.ended && (self.ahead.is_empty() || Instant::now() < until) {
            let Some(image) = self.decode_next(rgba) else { break };
            self.ahead.push_back(image);
        }
    }
}

fn convert(yuv: &openh264::decoder::DecodedYUV<'_>, rgba: &mut Vec<u8>) -> Option<Image> {
    let (w, h) = yuv.dimensions();
    rgba.resize(w * h * 4, 0);
    yuv.write_rgba8(rgba);
    let info = ImageInfo::new((w as i32, h as i32), ColorType::RGBA8888, AlphaType::Opaque, None);
    images::raster_from_data(&info, Data::new_copy(rgba), w * 4)
}

/// Starts playing `files` on a background thread; the returned picture follows the clips.
pub fn start(files: Vec<PathBuf>) -> SharedPicture {
    let shared: SharedPicture = Arc::default();
    if files.is_empty() {
        return shared; // nothing playable: the plain dark background stays
    }
    let out = shared.clone();
    std::thread::Builder::new()
        .name("background-video".into())
        .spawn(move || run(files, &out))
        .ok();
    shared
}

fn run(files: Vec<PathBuf>, out: &SharedPicture) {
    let mut files: Vec<Option<PathBuf>> = files.into_iter().map(Some).collect();
    let mut at = files.len() - 1;
    let (mut rgba, mut rgba_next) = (Vec::new(), Vec::new());

    let mut open_next = |now: Instant, files: &mut Vec<Option<PathBuf>>| -> Option<Clip> {
        for _ in 0..files.len() {
            at = (at + 1) % files.len();
            let Some(path) = files[at].clone() else { continue };
            match Clip::open(&path, now) {
                Ok(clip) => return Some(clip),
                Err(err) => {
                    tracing::warn!("skipping background video {}: {err}", path.display());
                    files[at] = None; // skipped until the next start
                }
            }
        }
        None
    };

    let Some(mut current) = open_next(Instant::now(), &mut files) else { return };
    let mut next: Option<(Clip, f64)> = None; // the following clip and the length of its fade
    let mut opening: Option<std::thread::JoinHandle<Clip>> = None; // the following clip, starting
    let mut shown: (Option<Image>, Option<Image>, f32) = (None, None, 0.);

    loop {
        let now = Instant::now();
        let t = current.elapsed(now);
        let front = current.frame_at(t, &mut rgba);
        let left = current.duration - t;

        // The ending clip is over: the running clip takes its place (or, without a following
        // clip, the order goes on with a plain cut).
        if current.ended || left <= 0. {
            // The following clip is still decoding its first frame: hold the last frame a moment
            // rather than wait for it here
            if next.is_none() && opening.as_ref().is_some_and(|h| !h.is_finished()) {
                std::thread::sleep(Duration::from_millis(5));
                continue;
            }
            let following = next.take().map(|(clip, _)| clip).or_else(|| opening.take()?.join().ok());
            current = match following {
                Some(clip) => clip,
                None => match open_next(now, &mut files) {
                    Some(clip) => clip,
                    None => return,
                },
            };
            continue;
        }
        // The following clip's first frame is ready: the dissolve starts over what is left.
        if opening.as_ref().is_some_and(std::thread::JoinHandle::is_finished) {
            next = opening.take().and_then(|h| h.join().ok()).map(|clip| (clip, left.max(0.05)));
        }

        let picture = match &mut next {
            Some((clip, fade)) => {
                let incoming = clip.frame_at(clip.elapsed(now), &mut rgba);
                // ease-in-out over what the ending clip has left
                let x = (1. - left / *fade).clamp(0., 1.);
                let eased = x * x * (3. - 2. * x);
                match incoming {
                    Some(incoming) => (Some(incoming), front, (1. - eased) as f32),
                    None => (front, None, 0.),
                }
            }
            None => (front, None, 0.),
        };

        let same = |a: &Option<Image>, b: &Option<Image>| match (a, b) {
            (Some(a), Some(b)) => a.unique_id() == b.unique_id(),
            (None, None) => true,
            _ => false,
        };
        if !same(&picture.0, &shown.0) || !same(&picture.1, &shown.1) || (picture.2 - shown.2).abs() > 0.004 {
            shown = picture.clone();
            if let Ok(mut out) = out.lock() {
                out.front = picture.0;
                out.fading = picture.1.map(|image| (image, picture.2));
                out.generation += 1;
                out.changed.notify_one();
            }
        }
        // The following clip's first frame (decoder start, key frame) can take two frames' time:
        // it is decoded on a thread of its own, so the running clip goes on meanwhile.
        if next.is_none() && opening.is_none() && FADE > 0. && left <= FADE && !current.ended {
            opening = open_next(now, &mut files).map(|mut clip| {
                std::thread::spawn(move || {
                    clip.prefetch(Instant::now(), &mut Vec::new());
                    clip
                })
            });
        }
        // The next frame of either clip is due at `wake`; until then both decode ahead, the two
        // of a dissolve on two threads (a frame of the larger clips averages over half a frame's
        // time to decode). Then sleep until it is due, rather than polling.
        let due = |c: &Clip| c.start + Duration::from_secs_f64((c.shown + 1).max(0) as f64 * c.frame_time);
        let wake = match &next {
            Some((clip, _)) if !clip.ended => due(&current).min(due(clip)),
            _ => due(&current),
        };
        std::thread::scope(|s| {
            if let Some((clip, _)) = &mut next {
                s.spawn(|| clip.prefetch(wake, &mut rgba_next));
            }
            current.prefetch(wake, &mut rgba);
        });
        std::thread::sleep(wake.saturating_duration_since(Instant::now()).min(Duration::from_secs_f64(current.frame_time)));
    }
}

#[cfg(test)]
mod tests {
    use super::{Clip, Instant, Path, natural_cmp};

    /// The bundled clips are High profile with B-frames: every sample must come out as a frame
    /// (with per-decode flushing OpenH264 failed after ~9 frames and the clip cut to the next).
    /// Every bundled clip, so a re-encode that the decoder cannot read fails here, not on a player's PC.
    #[test]
    fn bundled_clip_decodes_every_frame() {
        for path in super::list_videos(Path::new("videos")) {
            let mut clip = Clip::open(&path, Instant::now()).unwrap();
            let mut rgba = Vec::new();
            let mut frames = 0;
            while clip.decode_next(&mut rgba).is_some() {
                frames += 1;
            }
            assert_eq!(frames, clip.samples, "{}", path.display());
        }
    }

    #[test]
    fn play_order_compares_numbers_as_numbers() {
        let mut names = vec!["bg-10.mp4", "bg-2.mp4", "BG-1.mp4", "bg-02.mp4", "intro.mp4"];
        names.sort_by(|a, b| natural_cmp(a, b));
        assert_eq!(names, ["BG-1.mp4", "bg-2.mp4", "bg-02.mp4", "bg-10.mp4", "intro.mp4"]);
    }
}

