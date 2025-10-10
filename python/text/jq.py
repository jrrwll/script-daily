#!/usr/bin/env python3

import argparse
import json
import sys

"""
jq -r '.a'
jq -r '.a.b'
jq -r '.a[].b'
"""

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-r', '--json-path', type=str)
    args = parser.parse_args()
    json_path = args.json_path
    names = json_path.split('.')[1:]

    json_str = sys.stdin.read()
    obj = json.loads(json_str)

    cur_obj = obj
    for name in names:
        if not cur_obj:
            break
        if name.endswith('[]'):
            name = name[:-2]
        if isinstance(cur_obj, list):
            cur_obj = [isinstance(i, dict) and i.get(name) for i in cur_obj]
        else:
            cur_obj = isinstance(cur_obj, dict) and cur_obj.get(name)

    print(json.dumps(cur_obj, ensure_ascii=False, indent=4))
