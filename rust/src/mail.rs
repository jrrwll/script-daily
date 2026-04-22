use config::{Config, Environment};
use rexl::argparse::{ArgParserRunnable, FromArgs};
use rexl::mail::{MailMessageBuilder, MailSender};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, FromArgs)]
#[arg_parser(first_char)]
pub struct MailSendCli {
    pub subject: String,
    pub content: String,
    #[arg_parser(name = "H")]
    pub html: bool,
    #[arg_parser(name = "A")]
    pub attachments: Vec<String>,

    pub from: String,
    pub to: String,
    pub cc: Vec<String>,
    pub bcc: Vec<String>,
    pub reply_to: Vec<String>,
}

impl ArgParserRunnable for MailSendCli {
    fn run(self) {
        match self.run_anyway() {
            Ok(_) => {},
            Err(e) => {
                eprintln!("failed to send email:\n{}", e)
            }
        };
    }
}

impl MailSendCli {

    fn run_anyway(self) -> Result<(), Box<dyn std::error::Error>> {
        let env = MailEnv::parse()?;
        let sender: MailSender = env.try_into()?;

        let mut message_builder = MailMessageBuilder::builder()
            .subject(self.subject)
            .from(&self.from)?
            .to(&self.to)?;
        for ref cc in self.cc {
            message_builder = message_builder.cc(cc)?;
        }
        for ref bcc in self.bcc {
            message_builder = message_builder.bcc(bcc)?;
        }
        for ref reply_to in self.reply_to {
            message_builder = message_builder.reply_to(reply_to)?;
        }

        let message = message_builder.body_with_attachments(self.content, self.html, self.attachments)?;
        sender.send(&message)?;
        Ok(())
    }
}

#[derive(Debug, Default, Deserialize, Serialize)]
#[serde(default)]
struct MailEnv {
    url: Option<String>,
    smtp_host: Option<String>,
    starttls: bool,
    username: String,
    password: String,
}

impl MailEnv {

    fn parse() -> Result<Self, Box<dyn std::error::Error>> {
        let env_cfg = Config::builder()
            .add_source(Environment::default().prefix("RSTOOL_EMAIL_"))
            .build()?
            .try_deserialize::<Self>()
            .map_err(|e| format!("failed parse config from env: {}", e))?;
        Ok(env_cfg)
    }
}

impl TryFrom<MailEnv> for MailSender {
    type Error = Box<dyn std::error::Error>;

    fn try_from(value: MailEnv) -> Result<Self, Self::Error> {
        let credentials = Some((value.username, value.password));
        if let Some(smtp_host ) = value.smtp_host {
            let sender = if value.starttls {
                MailSender::from_starttls_relay(&smtp_host, credentials)
            } else {
                MailSender::from_relay(&smtp_host, credentials)
            }?;
            Ok(sender)
        } else if let Some(url ) = value.url {
            Ok(MailSender::from_url(&url, credentials)?)
        } else {
            Err("no smtp_host or url provided in os env".into())
        }
    }
}

