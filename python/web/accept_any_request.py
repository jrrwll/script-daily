import json

import uvicorn
from fastapi import FastAPI, APIRouter, Request

app = FastAPI()
app.router = APIRouter()

ok_response = {
    "code": 0,
    "success": True
}

@app.router.get("/", response_model=dict)
async def _get(request: Request):
    return await _request(request)

@app.router.post("/", response_model=dict)
async def _post(request: Request):
    return await _request(request)

@app.router.put("/", response_model=dict)
async def _put(request: Request):
    return await _request(request)

@app.router.delete("/", response_model=dict)
async def _delete(request: Request):
    return await _request(request)

async def _request(request: Request):
    print(f"\nPOST {request.url}")
    headers = [f"> {k}: {v}" for k, v in request.headers.items()]
    print("\n".join(headers))
    body = await request.body()
    body_str = body.decode()
    print(f">\n{body_str}\n>")

    is_json = request.headers.get("content-type", "").startswith("application/json")
    return {
        "data": json.loads(body_str) if is_json else body_str,
        **ok_response
    }

if __name__ == '__main__':
    uvicorn.run(app, host='0.0.0.0', port=5000)
