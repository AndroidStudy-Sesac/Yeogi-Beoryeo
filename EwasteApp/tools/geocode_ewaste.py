import json
import re
import time
import urllib.parse
import urllib.request
import urllib.error
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

LOCAL_PROPERTIES = ROOT / "local.properties"
INPUT_JSON = ROOT / "app" / "src" / "main" / "assets" / "ewaste_seoul.json"
OUTPUT_JSON = ROOT / "app" / "src" / "main" / "assets" / "ewaste_seoul_geocoded.json"


DISTRICT_CENTER = {
    "강남구": (37.5172, 127.0473),
    "강동구": (37.5301, 127.1238),
    "강북구": (37.6396, 127.0257),
    "강서구": (37.5509, 126.8495),
    "관악구": (37.4784, 126.9516),
    "광진구": (37.5384, 127.0823),
    "구로구": (37.4955, 126.8877),
    "금천구": (37.4569, 126.8955),
    "노원구": (37.6542, 127.0568),
    "도봉구": (37.6688, 127.0471),
    "동대문구": (37.5744, 127.0396),
    "동작구": (37.5124, 126.9393),
    "마포구": (37.5663, 126.9018),
    "서초구": (37.4836, 127.0327),
    "성동구": (37.5633, 127.0369),
    "성북구": (37.5894, 127.0167),
    "송파구": (37.5145, 127.1059),
    "양천구": (37.5169, 126.8664),
    "영등포구": (37.5264, 126.8962),
    "용산구": (37.5326, 126.9900),
    "은평구": (37.6027, 126.9291),
    "종로구": (37.5735, 126.9788),
    "중구": (37.5636, 126.9976),
    "중랑구": (37.6063, 127.0927),
}


def read_local_properties():
    props = {}

    if not LOCAL_PROPERTIES.exists():
        return props

    with open(LOCAL_PROPERTIES, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()

            if not line or line.startswith("#") or "=" not in line:
                continue

            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()

    return props


def clean_html(text):
    return re.sub(r"<.*?>", "", text or "")


def get_district(region):
    for district in DISTRICT_CENTER.keys():
        if district in region:
            return district
    return None


def normalize_local_coord(value):
    """
    네이버 지역 검색 API 좌표가 보통 정수 형태로 내려옵니다.
    예: 1270276217 -> 127.0276217
    """
    if value is None:
        return None

    try:
        number = float(value)
    except ValueError:
        return None

    if abs(number) > 1000:
        number = number / 10_000_000

    return number


def is_valid_seoul_coord(latitude, longitude):
    """
    서울 근처 좌표인지 대략 검사합니다.
    """
    if latitude is None or longitude is None:
        return False

    return 37.0 <= latitude <= 38.0 and 126.0 <= longitude <= 128.0


def search_local(query, search_id, search_secret):
    encoded_query = urllib.parse.quote(query)
    url = (
        "https://openapi.naver.com/v1/search/local.json"
        f"?query={encoded_query}&display=5&start=1&sort=random"
    )

    request = urllib.request.Request(url)
    request.add_header("X-Naver-Client-Id", search_id)
    request.add_header("X-Naver-Client-Secret", search_secret)
    request.add_header("Accept", "application/json")

    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            data = json.loads(response.read().decode("utf-8"))
            return data.get("items", [])

    except urllib.error.HTTPError as e:
        error_body = e.read().decode("utf-8", errors="ignore")
        print(f"\n  [지역검색 HTTP 오류] {e.code} / {error_body}")
        return []

    except Exception as e:
        print(f"\n  [지역검색 실패] {e}")
        return []


def geocode_address(address, ncp_id, ncp_secret):
    if not address:
        return None

    encoded_addr = urllib.parse.quote(address)
    url = f"https://maps.apigw.ntruss.com/map-geocode/v2/geocode?query={encoded_addr}"

    request = urllib.request.Request(url)
    request.add_header("X-NCP-APIGW-API-KEY-ID", ncp_id)
    request.add_header("X-NCP-APIGW-API-KEY", ncp_secret)
    request.add_header("Accept", "application/json")

    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            data = json.loads(response.read().decode("utf-8"))

        addresses = data.get("addresses", [])
        if not addresses:
            return None

        first = addresses[0]
        latitude = float(first["y"])
        longitude = float(first["x"])

        if is_valid_seoul_coord(latitude, longitude):
            return latitude, longitude

        return None

    except urllib.error.HTTPError as e:
        error_body = e.read().decode("utf-8", errors="ignore")
        print(f"\n  [지오코딩 HTTP 오류] {e.code} / {error_body}")
        return None

    except Exception as e:
        print(f"\n  [지오코딩 실패] {e}")
        return None


def score_item(item, store_name, region):
    title = clean_html(item.get("title", ""))
    address = item.get("address", "")
    road_address = item.get("roadAddress", "")
    category = item.get("category", "")

    district = get_district(region)
    full_text = f"{title} {address} {road_address} {category}"

    score = 0

    if district and district in full_text:
        score += 10

    store_compact = store_name.replace(" ", "")
    title_compact = title.replace(" ", "")

    if store_compact and store_compact in title_compact:
        score += 30

    for token in store_name.split():
        if token and token in title:
            score += 5

    # 서울 주소면 가산점
    if "서울" in full_text:
        score += 5

    return score


def choose_best_item(items, store_name, region):
    if not items:
        return None

    scored = [(score_item(item, store_name, region), item) for item in items]
    scored.sort(key=lambda x: x[0], reverse=True)

    best_score, best_item = scored[0]

    if best_score <= 0:
        return None

    return best_item


def get_coords_from_local_item(item):
    longitude = normalize_local_coord(item.get("mapx"))
    latitude = normalize_local_coord(item.get("mapy"))

    if is_valid_seoul_coord(latitude, longitude):
        return latitude, longitude

    return None


def main():
    print("🚀 네이버 지역 검색 API + 좌표 보정 변환을 시작합니다.")

    props = read_local_properties()

    ncp_id = props.get("NAVER_CLIENT_ID")
    ncp_secret = props.get("NAVER_CLIENT_SECRET")
    search_id = props.get("NAVER_SEARCH_CLIENT_ID")
    search_secret = props.get("NAVER_SEARCH_CLIENT_SECRET")

    if not all([ncp_id, ncp_secret, search_id, search_secret]):
        print("❌ local.properties에 아래 4개 키가 모두 있어야 합니다.")
        print("NAVER_CLIENT_ID")
        print("NAVER_CLIENT_SECRET")
        print("NAVER_SEARCH_CLIENT_ID")
        print("NAVER_SEARCH_CLIENT_SECRET")
        return

    with open(INPUT_JSON, "r", encoding="utf-8") as f:
        stores = json.load(f)

    result = []
    exact_count = 0
    address_geocode_count = 0
    approximate_count = 0
    fail_count = 0

    for index, store in enumerate(stores, start=1):
        store_name = store["storeName"]
        region = store["region"]
        district = get_district(region)

        queries = [
            f"{region} {store_name}",
            f"{district} {store_name}" if district else store_name,
            store_name,
            f"{store_name} 서울",
        ]

        print(f"[{index}/{len(stores)}] {region} / {store_name}", end=" -> ")

        selected_item = None

        for query in queries:
            items = search_local(query, search_id, search_secret)
            selected_item = choose_best_item(items, store_name, region)

            if selected_item:
                break

            time.sleep(0.1)

        new_store = store.copy()

        if selected_item:
            title = clean_html(selected_item.get("title", ""))
            address = selected_item.get("address", "")
            road_address = selected_item.get("roadAddress", "")
            local_coords = get_coords_from_local_item(selected_item)

            if local_coords:
                latitude, longitude = local_coords

                new_store["latitude"] = latitude
                new_store["longitude"] = longitude
                new_store["address"] = address
                new_store["roadAddress"] = road_address
                new_store["matchedName"] = title
                new_store["geocoded"] = True
                new_store["approximate"] = False
                new_store["source"] = "naver_local"

                exact_count += 1
                print(f"성공 / 지역검색 좌표 / {title}")

            else:
                coords = geocode_address(road_address or address, ncp_id, ncp_secret)

                if coords:
                    latitude, longitude = coords

                    new_store["latitude"] = latitude
                    new_store["longitude"] = longitude
                    new_store["address"] = address
                    new_store["roadAddress"] = road_address
                    new_store["matchedName"] = title
                    new_store["geocoded"] = True
                    new_store["approximate"] = False
                    new_store["source"] = "naver_geocoding"

                    address_geocode_count += 1
                    print(f"성공 / 주소 지오코딩 / {title}")

                else:
                    selected_item = None

        if not selected_item:
            if district:
                latitude, longitude = DISTRICT_CENTER[district]

                new_store["latitude"] = latitude
                new_store["longitude"] = longitude
                new_store["address"] = region
                new_store["roadAddress"] = ""
                new_store["matchedName"] = ""
                new_store["geocoded"] = False
                new_store["approximate"] = True
                new_store["source"] = "district_center"

                approximate_count += 1
                print(f"대체 / {district} 중심 좌표")

            else:
                new_store["latitude"] = None
                new_store["longitude"] = None
                new_store["address"] = region
                new_store["roadAddress"] = ""
                new_store["matchedName"] = ""
                new_store["geocoded"] = False
                new_store["approximate"] = False
                new_store["source"] = "failed"

                fail_count += 1
                print("실패 / 좌표 없음")

        result.append(new_store)
        time.sleep(0.15)

    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print("-" * 50)
    print(f"✅ 변환 완료: {OUTPUT_JSON}")
    print(f"정확 좌표 성공: {exact_count}개")
    print(f"주소 지오코딩 성공: {address_geocode_count}개")
    print(f"지역구 중심 대체: {approximate_count}개")
    print(f"완전 실패: {fail_count}개")


if __name__ == "__main__":
    main()