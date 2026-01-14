use rexl::argparse::{ArgParserRunnable, FromArgs};
use rsop::time::*;
#[test]
fn test_time() {
    let args = vec!["2020-02-02", "1d"]
        .iter().map(|x|x.to_string())
        .collect();
    let cli = TimeAddCli::from_args(args).unwrap();
    cli.run();
}