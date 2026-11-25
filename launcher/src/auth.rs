//! Login against the server's account system (README "Backend API").
//!
//! With `apiBaseUrl` empty in config.json the launcher runs in OFFLINE dev mode: any valid
//! username is accepted and no password is checked.

use serde_json::{Value, json};

use crate::config::Config;

#[derive(Debug, Clone)]
pub struct Session {
    pub username: String,
    pub session_token: Option<String>,
}

pub fn valid_username(name: &str) -> bool {
    (3..=16).contains(&name.len()) && name.chars().all(|c| c.is_ascii_alphanumeric() || c == '_')
}

fn http() -> reqwest::Client {
    reqwest::Client::builder()
        .user_agent(concat!("NewgenLauncher/", env!("CARGO_PKG_VERSION")))
        .timeout(std::time::Duration::from_secs(30))
        .build()
        .expect("http client")
}

/// POSTs JSON; the API's `error` field is shown to the player as-is.
async fn post(url: &str, body: Value) -> Result<Value, String> {
    let response = http()
        .post(url)
        .json(&body)
        .send()
        .await
        .map_err(|_| crate::i18n::tr(crate::i18n::S::LoginUnreachable).to_string())?;
    let status = response.status();
    let data: Value = response.json().await.unwrap_or_else(|_| json!({}));
    if !status.is_success() {
        return Err(data
            .get("error")
            .and_then(Value::as_str)
            .map(str::to_string)
            .unwrap_or_else(|| crate::i18n::trf(crate::i18n::S::LoginServerError, &[&status.as_u16()])));
    }
    Ok(data)
}

pub async fn login(config: &Config, username: &str, password: &str) -> Result<Session, String> {
    let username = username.trim();
    if !valid_username(username) {
        return Err(crate::i18n::tr(crate::i18n::S::BadUsername).into());
    }
    if !config.auth_enabled() {
        return Ok(Session { username: username.to_string(), session_token: None });
    }
    if password.is_empty() {
        return Err(crate::i18n::tr(crate::i18n::S::EnterPassword).into());
    }
    let data = post(
        &format!("{}/launcher/login", config.api_base_url.trim_end_matches('/')),
        json!({ "username": username, "password": password }),
    )
    .await?;
    Ok(Session {
        username: data
            .get("username")
            .and_then(Value::as_str)
            .unwrap_or(username)
            .to_string(),
        session_token: data.get("sessionToken").and_then(Value::as_str).map(str::to_string),
    })
}

/// A fresh one-time join token right before each launch; `None` in offline dev mode.
pub async fn join_token(config: &Config, session: &Session) -> Result<Option<String>, String> {
    if !config.auth_enabled() {
        return Ok(None);
    }
    let data = post(
        &format!("{}/launcher/join-token", config.api_base_url.trim_end_matches('/')),
        json!({ "sessionToken": session.session_token }),
    )
    .await?;
    Ok(data.get("joinToken").and_then(Value::as_str).map(str::to_string))
}

#[cfg(test)]
mod tests {
    use super::valid_username;

    #[test]
    fn usernames() {
        assert!(valid_username("Steve_01"));
        assert!(!valid_username("ab"));
        assert!(!valid_username("seventeen_chars_x"));
        assert!(!valid_username("has space"));
        assert!(!valid_username("ümlaut"));
    }
}
