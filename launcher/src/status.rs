//! Players online, from the website's own statistics (`config.statusUrl`, the JSON the
//! newgenmc.net hero stats bar reads: `{ "online": 25, "totalJoined": 21178, … }`).

use std::time::Duration;

/// The `online` count of the site's statistics JSON.
pub fn parse_online(json: &[u8]) -> Option<u64> {
    serde_json::from_slice::<serde_json::Value>(json).ok()?.get("online")?.as_u64()
}

/// Players online now; None when the site cannot be reached or answers something else.
pub async fn players_online(url: &str) -> Option<u64> {
    if url.trim().is_empty() {
        return None;
    }
    let client = reqwest::Client::builder()
        .user_agent(concat!("NewgenLauncher/", env!("CARGO_PKG_VERSION")))
        .timeout(Duration::from_secs(10))
        .build()
        .ok()?;
    let response = client.get(url).send().await.ok()?.error_for_status().ok()?;
    parse_online(&response.bytes().await.ok()?)
}

#[cfg(test)]
mod tests {
    use super::parse_online;

    #[test]
    fn reads_online_from_the_site_sample() {
        let sample = include_bytes!("../tests/data/site-stats.json");
        assert_eq!(parse_online(sample), Some(25));
    }

    #[test]
    fn anything_else_is_no_count() {
        assert_eq!(parse_online(b"<html>"), None);
        assert_eq!(parse_online(br#"{"online":"many"}"#), None);
        assert_eq!(parse_online(br#"{"players":3}"#), None);
    }
}
