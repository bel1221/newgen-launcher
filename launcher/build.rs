// Windows: the launcher's .exe gets NewGen's icon (assets/newgen/icon.png, converted to .ico).
fn main() {
    println!("cargo:rerun-if-changed=assets/newgen/icon.png");
    #[cfg(windows)]
    {
        let out = std::path::PathBuf::from(std::env::var("OUT_DIR").unwrap()).join("icon.ico");
        let icon = image::open("assets/newgen/icon.png").expect("assets/newgen/icon.png");
        icon.resize(256, 256, image::imageops::FilterType::Lanczos3)
            .save_with_format(&out, image::ImageFormat::Ico)
            .expect("write icon.ico");
        let mut res = winresource::WindowsResource::new();
        res.set_icon(out.to_str().unwrap());
        res.set("ProductName", "Newgen Launcher");
        res.set("FileDescription", "Newgen Launcher");
        res.compile().expect("embed Windows icon resource");
    }
}
