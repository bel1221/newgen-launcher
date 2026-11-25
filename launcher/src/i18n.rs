//! English / Vietnamese. Every launcher string is one `S` with both texts side by side; `{}` in
//! a text is filled in order by `fill`. Text from the server's API and the website is shown as
//! it comes.

use std::sync::atomic::{AtomicU8, Ordering};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Lang {
    En,
    Vi,
}

macro_rules! strings {
    ($($name:ident: $en:literal, $vi:literal;)*) => {
        #[derive(Clone, Copy, Debug, PartialEq, Eq)]
        pub enum S { $($name),* }
        impl S {
            #[cfg(test)]
            pub const ALL: &[S] = &[$(S::$name),*];
            fn texts(self) -> (&'static str, &'static str) {
                match self { $(S::$name => ($en, $vi)),* }
            }
        }
    };
}

strings! {
    // Login
    LoginSubtitle: "Log in with your NewGen account", "Đăng nhập bằng tài khoản NewGen";
    AccountName: "Account name", "Tên tài khoản";
    Password: "Password", "Mật khẩu";
    NotNeeded: "Not needed", "Không cần";
    LogIn: "Log in", "Đăng nhập";
    LoggingIn: "Logging in…", "Đang đăng nhập…";
    // Home
    Play: "Play", "Chơi";
    Launching: "Launching…", "Đang khởi động…";
    Playing: "Playing", "Đang chơi";
    Updating: "Updating {}%", "Đang cập nhật {}%";
    PlayersOnline: "{} players online", "{} người chơi đang online";
    GameLog: "Game log", "Nhật ký game";
    CloseGameLog: "Close game log", "Đóng nhật ký game";
    OpenSection: "Open on newgenmc.net", "Mở trên newgenmc.net";
    // Settings
    Settings: "Settings", "Cài đặt";
    CloseSettings: "Close settings", "Đóng cài đặt";
    Memory: "Memory (RAM)", "Bộ nhớ (RAM)";
    KeepOpen: "Keep launcher open", "Giữ launcher mở";
    KeepOpenHint: "When off, the launcher minimizes when the game starts", "Khi tắt, launcher sẽ thu nhỏ khi game khởi động";
    OpenGameFolder: "Open game folder", "Mở thư mục game";
    OpenLogs: "Open logs", "Mở thư mục log";
    Language: "Language", "Ngôn ngữ";
    // Navbar
    LogOut: "Log out", "Đăng xuất";
    Discord: "Discord", "Discord";
    Website: "Website", "Trang web";
    Minimize: "Minimize", "Thu nhỏ";
    Close: "Close", "Đóng";
    // Launch steps and game
    CheckingFiles: "Checking server files...", "Đang kiểm tra file của server...";
    GettingTicket: "Getting login ticket...", "Đang lấy vé đăng nhập...";
    StartingMinecraft: "Starting Minecraft...", "Đang khởi động Minecraft...";
    GameRunning: "Game running", "Game đang chạy";
    GameClosed: "Game closed (code {})", "Game đã đóng (mã {})";
    Unknown: "unknown", "không rõ";
    AlreadyRunning: "The game is already running", "Game đang chạy rồi";
    ModsNotAllowed: "Adding Mods is not Allowed, please remove \"{}\" to continue using Newgen Launcher", "Không được phép thêm mod, vui lòng gỡ \"{}\" để tiếp tục dùng Newgen Launcher";
    ModsMissing: "The launcher mods are missing. Reinstall the launcher.", "Thiếu mod của launcher. Hãy cài lại launcher.";
    CheckingFile: "Checking {}", "Đang kiểm tra {}";
    FilesUpToDate: "Files up to date", "File đã được cập nhật";
    DownloadingJava: "Downloading Java", "Đang tải Java";
    Downloading: "Downloading {}", "Đang tải {}";
    DownloadingGameFiles: "Downloading game files", "Đang tải file game";
    // Errors the launcher writes
    LoginUnreachable: "Could not reach the login server", "Không kết nối được máy chủ đăng nhập";
    LoginServerError: "Login server error ({})", "Lỗi máy chủ đăng nhập ({})";
    BadUsername: "Username must be 3-16 characters: letters, numbers or _", "Tên phải dài 3-16 ký tự: chữ cái, số hoặc _";
    EnterPassword: "Enter your password", "Hãy nhập mật khẩu";
    UnsafePath: "Unsafe path in manifest: {}", "Đường dẫn không an toàn trong manifest: {}";
    FileListFailed: "Could not download the server file list", "Không tải được danh sách file của server";
    DownloadFailed: "Could not download {}", "Không tải được {}";
    DownloadCorrupted: "Download was corrupted: {}", "File tải về bị lỗi: {}";
    JavaBroken: "The Java set in config.json does not work ({}): {}", "Java trong config.json không chạy được ({}): {}";
    JavaTooOld: "The Java set in config.json is Java {}, but Minecraft {} needs Java {} or newer", "Java trong config.json là Java {}, nhưng Minecraft {} cần Java {} trở lên";
    JavaFailed: "Could not get Java {}: {}", "Không lấy được Java {}: {}";
    GameFolderFailed: "Could not create the game folder: {}", "Không tạo được thư mục game: {}";
    MinecraftNotFound: "Could not find Minecraft {}: {}", "Không tìm thấy Minecraft {}: {}";
    FabricFailed: "Could not get Fabric {} (check versions in config.json): {}", "Không lấy được Fabric {} (kiểm tra phiên bản trong config.json): {}";
    VersionFileFailed: "Could not download the game's version file: {}", "Không tải được file phiên bản của game: {}";
    MinecraftDownloadFailed: "Could not download Minecraft: {}", "Không tải được Minecraft: {}";
    MinecraftStartFailed: "Minecraft failed to start ({}). Check the game log.", "Minecraft không khởi động được ({}). Hãy xem nhật ký game.";
    // What's new
    WhatsNew: "What's new in v{}", "Có gì mới trong bản v{}";
    SeeAll: "See all", "Xem tất cả";
    ShowLess: "Show less", "Thu gọn";
    // Crash panel
    CrashTitle: "Minecraft closed unexpectedly", "Minecraft đã đóng bất ngờ";
    CrashHint: "The last lines of the game log:", "Những dòng cuối trong nhật ký game:";
    CopyLog: "Copy log", "Sao chép log";
    OpenCrashReports: "Open crash reports folder", "Mở thư mục báo cáo lỗi";
    CopyFailed: "Could not copy the log", "Không sao chép được log";
    OpenFolderFailed: "Could not open the folder", "Không mở được thư mục";
}

static LANG: AtomicU8 = AtomicU8::new(0);

impl Lang {
    pub fn text(self, s: S) -> &'static str {
        let (en, vi) = s.texts();
        match self {
            Self::En => en,
            Self::Vi => vi,
        }
    }

    pub fn code(self) -> &'static str {
        match self {
            Self::En => "en",
            Self::Vi => "vi",
        }
    }

    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "en" => Some(Self::En),
            "vi" => Some(Self::Vi),
            _ => None,
        }
    }

    /// 21179 → "21,179" (en) / "21.179" (vi)
    pub fn number(self, n: u64) -> String {
        let sep = if self == Self::Vi { '.' } else { ',' };
        let digits = n.to_string();
        let mut out = String::new();
        for (i, c) in digits.chars().enumerate() {
            if i > 0 && (digits.len() - i) % 3 == 0 {
                out.push(sep);
            }
            out.push(c);
        }
        out
    }
}

/// The language now, for code off the UI thread (launch steps, errors).
pub fn lang() -> Lang {
    if LANG.load(Ordering::Relaxed) == 1 { Lang::Vi } else { Lang::En }
}

pub fn set_lang(lang: Lang) {
    LANG.store(u8::from(lang == Lang::Vi), Ordering::Relaxed);
}

/// The text of `s` in the current language.
pub fn tr(s: S) -> &'static str {
    lang().text(s)
}

/// `{}` in `text` replaced by `args` in order.
pub fn fill(text: &str, args: &[&dyn std::fmt::Display]) -> String {
    let mut out = String::new();
    let mut parts = text.split("{}");
    out.push_str(parts.next().unwrap_or_default());
    for (i, part) in parts.enumerate() {
        if let Some(arg) = args.get(i) {
            out.push_str(&arg.to_string());
        }
        out.push_str(part);
    }
    out
}

/// A line the launcher shows later (status, error): kept in both languages, so it follows a
/// language switch after it was written. Text from elsewhere (API, game) is the same in both.
#[derive(Clone, Debug, PartialEq)]
pub struct Text {
    en: String,
    vi: String,
}

impl Text {
    pub fn key(s: S, args: &[&dyn std::fmt::Display]) -> Self {
        Self { en: fill(Lang::En.text(s), args), vi: fill(Lang::Vi.text(s), args) }
    }

    /// `s` with its one blank filled by the word `word`, each in its own language.
    pub fn with_word(s: S, word: S) -> Self {
        Self { en: fill(Lang::En.text(s), &[&Lang::En.text(word)]), vi: fill(Lang::Vi.text(s), &[&Lang::Vi.text(word)]) }
    }

    pub fn raw(text: impl Into<String>) -> Self {
        let text = text.into();
        Self { en: text.clone(), vi: text }
    }

    pub fn get(&self, lang: Lang) -> &str {
        if lang == Lang::Vi { &self.vi } else { &self.en }
    }
}

/// `tr(s)` with its `{}` filled.
pub fn trf(s: S, args: &[&dyn std::fmt::Display]) -> String {
    fill(tr(s), args)
}

/// Saved choice, else Vietnamese when Windows' display language is Vietnamese, else English.
pub fn initial(saved: Option<&str>) -> Lang {
    saved.and_then(Lang::from_code).unwrap_or_else(|| if system_is_vietnamese() { Lang::Vi } else { Lang::En })
}

#[cfg(windows)]
fn system_is_vietnamese() -> bool {
    unsafe extern "system" {
        fn GetUserDefaultUILanguage() -> u16;
    }
    // Primary language id of the LANGID; LANG_VIETNAMESE = 0x2A
    (unsafe { GetUserDefaultUILanguage() } & 0x3FF) == 0x2A
}

#[cfg(not(windows))]
fn system_is_vietnamese() -> bool {
    std::env::var("LANG").is_ok_and(|l| l.starts_with("vi"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn every_string_has_both_languages_with_the_same_blanks() {
        for &s in S::ALL {
            let (en, vi) = s.texts();
            assert!(!en.trim().is_empty() && !vi.trim().is_empty(), "{s:?} is missing a translation");
            assert_eq!(en.matches("{}").count(), vi.matches("{}").count(), "{s:?}: different number of {{}}");
        }
    }

    #[test]
    fn numbers_and_blanks() {
        assert_eq!(Lang::En.number(21179), "21,179");
        assert_eq!(Lang::Vi.number(21179), "21.179");
        assert_eq!(Lang::En.number(25), "25");
        assert_eq!(Lang::En.number(1_000_000), "1,000,000");
        assert_eq!(fill(Lang::Vi.text(S::Updating), &[&42]), "Đang cập nhật 42%");
        assert_eq!(Lang::from_code("vi"), Some(Lang::Vi));
        assert_eq!(initial(Some("en")), Lang::En);
    }
}
