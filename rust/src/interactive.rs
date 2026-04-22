use home::home_dir;
use rustyline::error::ReadlineError;
use rustyline::{DefaultEditor, Result};
use std::fs;
use std::path::PathBuf;

#[derive(Debug, PartialEq, Clone, Copy)]
pub enum Context {
    Root,
    Timestamp,
    Cron,
    TimeDiff,
}

impl Context {
    pub fn prompt(&self) -> &str {
        match self {
            Context::Root => ">>> ",
            Context::Timestamp => "timestamp >>> ",
            Context::Cron => "cron >>> ",
            Context::TimeDiff => "time diff >>> ",
        }
    }
}

fn get_history_path() -> Option<PathBuf> {
    home_dir().map(|mut path| {
        path.push(".config");
        path.push("rstool");
        if let Err(e) = fs::create_dir_all(&path) {
            eprintln!("Failed to create diretory {:?}: {}", path, e);
        }
        path.join("history.txt")
    })
}

pub fn run_interactive() -> Result<()> {
    let mut rl = DefaultEditor::new()?;
    let mut current_ctx = Context::Root;

    let history_path = get_history_path();
    if let Some(ref path) = history_path {
        if rl.load_history(path).is_err() {
            println!("No previous history found at {:?}", path);
        }
    }

    loop {
        let readline = rl.readline(current_ctx.prompt());

        match readline {
            Ok(line) => {
                let input = line.trim();
                if input.is_empty() {
                    continue;
                }
                rl.add_history_entry(input)?;

                match input {
                    "ts" => current_ctx = Context::Timestamp,
                    "cron" => current_ctx = Context::Cron,
                    "td" => current_ctx = Context::TimeDiff,

                    "exit" => {
                        if current_ctx != Context::Root {
                            current_ctx = Context::Root;
                        } else {
                            break;
                        }
                    }
                    "quit" => {
                        break;
                    }
                    _ => handle_command(&current_ctx, input),
                }
            }
            Err(ReadlineError::Interrupted) => break,
            Err(ReadlineError::Eof) => break,
            _ => {}
        }
    }

    if let Some(ref path) = history_path {
        rl.save_history(path)?;
        println!("History saved to {:?}", path);
    }
    Ok(())
}

fn handle_command(ctx: &Context, cmd: &str) {
    match ctx {
        Context::Root => println!("Running global command: {}", cmd),
        Context::Timestamp => println!("Evaluating Timestamp: {}", cmd),
        Context::Cron => println!("Parsing Cron expression: {}", cmd),
        Context::TimeDiff => println!("Updating Time Diff: {}", cmd),
    }
}
