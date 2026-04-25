use chrono::{DateTime, Local, NaiveDateTime};
use rexl::argparse::{ArgParserRunnable, FromArgs};
use rexl::time::{HumanDuration, parse_duration, parse_datetime};

#[derive(Debug, FromArgs)]
pub struct TimeAddCli {
    #[arg_parser(position = 0)]
    pub time: String,
    #[arg_parser(position = 1)]
    pub duration: String,
    
}

impl ArgParserRunnable for TimeAddCli {
    fn run(self) {
        let Some(time) = parse_datetime(&self.time) else {
            eprintln!("invalid datetime format: {}", &self.time);
            return;
        };
        // let duration = HumanDuration::parse(self.duration).unwrap();

        let output = time.format("%Y-%m-%d");
        println!("{}", output);
    }
}

#[derive(Debug, FromArgs)]
pub struct TimeDiffCli {
    #[arg_parser(position = 0)]
    pub time1: String,
    #[arg_parser(position = 1)]
    pub time2: String,
}

impl ArgParserRunnable for TimeDiffCli {
    fn run(self) {
        println!("{:?}", &self);

        let time1 = NaiveDateTime::parse_from_str(
            &self.time1, "%Y-%m-%d").unwrap();
        let time2 = NaiveDateTime::parse_from_str(
            &self.time2, "%Y-%m-%d").unwrap();
        let diff = time1 - time2;
        println!("[{}d, {}h, {}m, {}s]",
                 diff.num_days(), diff.num_hours(),
                 diff.num_minutes(), diff.num_seconds()
        );
    }
}

#[derive(Debug, FromArgs)]
pub struct TimeNowCli {
    #[arg_parser(name = "ts")]
    pub timestamp: bool,
    #[arg_parser(name = "uts")]
    pub unix_timestamp: bool,
    #[arg_parser(name = "2822")]
    pub rfc2822: bool,
    #[arg_parser(name = "rfc,3339")]
    pub rfc3339: bool,
    #[arg_parser(name = "V")]
    pub verbose: bool,
}

impl ArgParserRunnable for TimeNowCli {
    fn run(self) {
        let now = Local::now();

        let mut idents: Vec<&str> = vec![];
        let mut outputs: Vec<String> = vec![];

        idents.push("timestamp_millis : ");
        outputs.push(format!("{}", now.timestamp_millis()));
        if self.timestamp {
            println!("{}", outputs[outputs.len() - 1]);
            return;
        }

        idents.push("timestamp        : ");
        outputs.push(format!("{}", now.timestamp()));
        if self.unix_timestamp {
            println!("{}", outputs[outputs.len() - 1]);
            return;
        }

        idents.push("rfc3339          : ");
        outputs.push(format!("{}", now.to_rfc3339()));
        if self.rfc3339 {
            println!("{}", outputs[outputs.len() - 1]);
            return;
        }

        idents.push("rfc2822          : ");
        outputs.push(format!("{}", now.to_rfc2822()));
        if self.rfc2822 {
            println!("{}", outputs[outputs.len() - 1]);
            return;
        }

        idents.insert(0, "typical          : ");
        outputs.insert(0, format!("{}", now.format("%Y-%m-%d %H:%M:%S%.3f")));
        if !self.verbose {
            println!("{}", outputs[0]);
            return;
        }

        for (i, ident) in idents.iter().enumerate() {
            println!("{}{}", ident, outputs[i]);
        }
    }
}

#[derive(Debug, FromArgs)]
#[arg_parser(first_char)]
pub struct TimestampCli {
    #[arg_parser(position = 0)]
    pub timestamp: i64,
    pub epoch: bool
}

impl ArgParserRunnable for TimestampCli {
    fn run(self) {
        let mut timestamp = self.timestamp;
        let mut nanos = 0;
        if !self.epoch {
            timestamp = timestamp / 1000;
            nanos = (timestamp % 1000) * 1000_000;
        }

        let Some(dt) = DateTime::from_timestamp(timestamp, nanos as u32) else {
            eprintln!("invalid timestamp: {}", timestamp);
            return;
        };
        println!("{}", dt);
    }
}
