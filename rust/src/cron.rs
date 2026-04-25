use cron::Schedule;
use chrono::Local;
use std::str::FromStr;
use rexl::argparse::{ArgParserRunnable, FromArgs};

#[derive(Debug, FromArgs)]
#[arg_parser(first_char)]
pub struct CronCli {
    #[arg_parser(position = 0)]
    pub expression: String,
    pub nums: u16,
}

impl ArgParserRunnable for CronCli {
    fn run(self) {
        let mut expression = self.expression;
        let nums = if self.nums == 0 { 10 } else { self.nums };
        let width = nums.ilog10() + 1;

        let cron_digit = expression.chars().filter(|&c| c == ' ').take(6).count() + 1;
        if cron_digit == 5 {
            expression = format!("0 {} *", &expression);
        }

        let schedule = match Schedule::from_str(&expression) {
            Ok(v) => v,
            Err(e) => {
                eprintln!("Failed to parse cron expression: {:?}", e);
                return;
            }
        };
        
        for (i, datetime) in schedule.upcoming(Local).take(nums as usize).enumerate() {
            println!("{:0width$}: {}", i + 1, datetime.format("%Y-%m-%d %H:%M:%S"), width = width as usize);
        }
    }
}
