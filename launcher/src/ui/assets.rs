//! Files built into the launcher: OneLauncher's fonts, icons and theme, and NewGen's logo, icon
//! and weapon models (`launcher/assets/`).

#[derive(rust_embed::RustEmbed)]
#[folder = "assets/"]
pub struct Assets;

impl Assets {
    pub fn get_bytes(path: &str) -> Option<bytes::Bytes> {
        Assets::get(path).map(|file| match file.data {
            std::borrow::Cow::Borrowed(slice) => bytes::Bytes::from_static(slice),
            std::borrow::Cow::Owned(vec) => bytes::Bytes::from(vec),
        })
    }
}
