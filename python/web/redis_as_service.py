import argparse
from typing import Any

from pydantic import BaseModel
import redis
import uvicorn
from fastapi import FastAPI, APIRouter

app = FastAPI()
app.router = APIRouter()


def ok_response(data: Any):
    return {
        "code": 0,
        "success": True,
        "data": data,
    }


client: redis.Redis | None = None


### api
class GetManyParam(BaseModel):
    keys: list[str]


class GetManyResult(BaseModel):
    key: str
    type: str
    value: Any


@app.router.post("/api/get_many", response_model=dict)
async def _get_many(request: GetManyParam):
    keys = request.keys
    values: list[GetManyResult] = []
    for key in keys:
        typ = client.type(key)
        if typ == "string":
            value = client.get(key)
        elif typ == "hash":
            value = client.hscan(key, 0, count=10)
        elif typ == "list":
            value = client.lrange(key, 0, 10)
        elif typ == "set":
            value = client.sscan(key, 0, count=10)
        elif typ == "zset":
            value = client.zrange(key, 0, 10)
        else:
            value = None
        values.append(GetManyResult(key=key, type=typ, value=value))

    return ok_response(values)


@app.router.delete("/api/del/{key}", response_model=dict)
async def _del(key: str):
    typ = client.type(key)
    if not typ or typ == 'none':
        return ok_response(False)
    res = client.delete(key)
    return ok_response(res)


### directly commands
@app.router.get("/type/{key}", response_model=dict)
async def _type(key: str):
    data = client.type(key)
    return ok_response(data)


@app.router.get("/get/{key}", response_model=dict)
async def _get(key: str):
    data = client.get(key)
    return ok_response(data)


@app.router.get("/hget/{key}/{field}", response_model=dict)
async def _hgetall(key: str, field: str):
    data = client.hget(key, field)
    return ok_response(data)


@app.router.get("/hgetall/{key}", response_model=dict)
async def _hgetall(key: str):
    data = client.hgetall(key)
    return ok_response(data)


@app.router.get("/hscan/{key}/{cursor}/{count}", response_model=dict)
async def _hscan(key: str, cursor: int, count: int):
    data = client.hscan(key, cursor, count=count)
    return ok_response(data)


@app.router.get("/lrange/{key}/{start}/{end}", response_model=dict)
async def _lrange(key: str, start: int, end: int):
    data = client.lrange(key, start, end)
    return ok_response(data)


@app.router.get("/lrange/{key}", response_model=dict)
async def _lrange_all(key: str):
    data = client.lrange(key, 0, -1)
    return ok_response(data)


@app.router.get("/smembers/{key}", response_model=dict)
async def _smembers(key: str):
    data = client.smembers(key)
    return ok_response(data)


@app.router.get("/sscan/{key}/{cursor}/{count}", response_model=dict)
async def _sscan(key: str, cursor: int, count: int):
    data = client.sscan(key, cursor, count=count)
    return ok_response(data)


@app.router.get("/zrange/{key}/{start}/{end}", response_model=dict)
async def _zrange(key: str, start: int, end: int):
    data = client.zrange(key, start, end)
    return ok_response(data)


@app.router.get("/zrange/{key}", response_model=dict)
async def _zrange_all(key: str):
    data = client.zrange(key, 0, -1)
    return ok_response(data)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-H", "--host", dest="host", default="127.0.0.1")
    parser.add_argument("-p", "--port", dest="port", type=int, default=6379)
    parser.add_argument("-P", "--password", dest="password", required=False)
    parser.add_argument("-d", "--db", dest="db", type=int, default=0)

    args = parser.parse_args()

    client = redis.Redis(
        host=args.host,
        port=args.port,
        password=args.password,
        db=args.db,
        decode_responses=True)
    pong = client.ping()
    print(f"redis ping: {pong}")

    uvicorn.run(app, host='0.0.0.0', port=5000)
