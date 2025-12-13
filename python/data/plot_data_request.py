import argparse
import json

import httpx
import jsonpath_ng
import pandas as pd
import plotly.graph_objects as go
from plotly.subplots import make_subplots

"""
uv add pandas plotly
uv add "httpx[http2]" jsonpath-ng
"""

# min-max 归一化
def minmax_scale_pd(s: pd.Series) -> pd.Series:
    return (s - s.min()) / (s.max() - s.min())


def plot_and_show(data: list[dict], x_col: str, y_cols: list[str], title: str):
    df = pd.DataFrame(data)
    # df[x_col] = pd.to_datetime(df[x_col])
    for col in y_cols:
        df[col] = pd.to_numeric(df[col])

    df_norm = df.copy()
    raw_dict = {} # as customdata
    for col in y_cols:
        raw_dict[col] = df[col]
        df_norm[col] = minmax_scale_pd(df[col])

    fig = make_subplots()
    # 归一化曲线
    for col in y_cols:
        fig.add_trace(
            go.Scatter(x=df_norm[x_col], y=df_norm[col],
                       mode='lines+markers',
                       name=col,
                       customdata=raw_dict[col],
                       hovertemplate=(
                           f"<b>{col}</b>"
                           ": %{customdata:,}<extra></extra>"
                       ),
                       ),
            secondary_y=False, # 左侧 y 轴
        )
    # 轴标题
    fig.update_yaxes(title_text="Normalized 0-1", secondary_y=False)
    fig.update_layout(title=title,
                      hovermode="x unified",
                      template="plotly_white",
                      colorway=["#636EFA","#EF553B","#00CC96","#AB63FA"]
                      ) # template: plotly_dark, ggplot2, seaborn
    fig.show()


def fetch_data(url: str, request_method: str, header_list: list[str], request_data: str):
    if request_data and request_method == 'GET':
        request_method = "POST"

    headers = {}
    for header_str in (header_list or []):
        if ':' in header_str:
            key, value = header_str.split(':', 1)
            headers[key.strip()] = value.strip()


    print(f">{request_method} {url}")
    print("\n".join([f">{k}: {v}" for k, v in headers.items()]))
    print(f">\n{request_data}\n>")

    # `http2=True` also works on http/1.1
    with httpx.Client(http2=True) as client:
        response = client.request(request_method, url, headers=headers, json=json.loads(request_data))
        json_str = response.text

    print(f"< {response.http_version} {response.status_code} {response.reason_phrase}")
    print("\n".join([f"<{k}: {v}" for k, v in response.headers.items()]))
    print(f"<\n{json_str}\n<")
    if response.status_code != 200:
        raise Exception(f"Failed to fetch data: {json_str}")

    data = json.loads(json_str)
    if (not data or data.get("code", 0) not in [0, 200]
            or data.get("success", False) is False):
        raise Exception(f"Failed to fetch data: {json_str}")
    return data


def extract_data(json_str, extract_jsonpath: str):
    if isinstance(json_str, str):
        data = json.loads(json_str)
    else:
        data = json_str

    expr = jsonpath_ng.parse(extract_jsonpath)
    matches = [match.value for match in expr.find(data)]
    if not matches:
        raise Exception(f"Not found any data via jsonpath {extract_jsonpath}: {json_str}")
    if len(matches) > 1:
        raise Exception(f"Found more than one data via jsonpath {extract_jsonpath}: {json_str}")
    return matches[0]


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    # http request
    parser.add_argument("--url", help="URL to fetch data")
    parser.add_argument("--request", "-X", default="GET",
                        help="Specify request method (GET, POST, PUT, DELETE, etc.)")
    parser.add_argument("--header","-H",  action="append",
                        help="Pass custom header(s) to server")
    parser.add_argument("--data", "-d",
                        help="HTTP POST data")
    # local file
    parser.add_argument("--file", "-f",
                        help="Local file path to fetch data")

    parser.add_argument("--jsonpath", "-p", "--path", help="Jsonpath to extract data")
    parser.add_argument("--title", "-t", help="Plot title")
    parser.add_argument("-x", "-x-col", "--x-column", required=True,
                        help="X column name")
    parser.add_argument("-y",  "-y-col", "--y-column", nargs="+", required=True,
                        help="Y column names")
    args = parser.parse_args()

    if not args.url and not args.file:
        raise Exception("Please specify url or file")

    if not args.file:
        response_data = fetch_data(args.url, args.request, args.header, args.data)
    else:
        with open(args.file, "r") as f:
            response_data = json.load(f)
        if not response_data:
            raise Exception(f"Failed to load file: {args.file}")

    target_data = extract_data(response_data, args.jsonpath)
    plot_and_show(target_data, args.x_column, args.y_column, args.title)
