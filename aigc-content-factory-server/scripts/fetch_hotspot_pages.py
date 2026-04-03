import argparse
import json
import re
from pathlib import Path

from playwright.sync_api import sync_playwright


def fetch_weibo(page):
    page.goto("https://s.weibo.com/top/summary", wait_until="domcontentloaded", timeout=60000)
    page.wait_for_timeout(4000)
    rows = page.locator("table tbody tr")
    items = []
    for index in range(min(rows.count(), 20)):
        row = rows.nth(index)
        text = row.inner_text().strip()
        if not text:
            continue
        anchors = row.locator("a")
        if anchors.count() == 0:
            continue
        title = anchors.first.inner_text().strip()
        if not title or title == "更多":
            continue
        score_match = re.search(r"(\d{4,})", text)
        items.append({
            "id": f"weibo-{index + 1}",
            "title": title,
            "summary": text[:120],
            "score": int(score_match.group(1)) if score_match else 0,
        })
    return items


def fetch_zhihu(page, profile_dir):
    context = page.context
    page.goto("https://www.zhihu.com/hot", wait_until="domcontentloaded", timeout=60000)
    page.wait_for_timeout(5000)
    body_text = page.locator("body").inner_text(timeout=5000)
    if "验证码登录" in body_text and "获取短信验证码" in body_text:
        return []
    cards = page.locator("[data-za-detail-view-element_name='Title'], section a[href*='/question/'], div.HotItem-content a")
    items = []
    seen = set()
    for index in range(min(cards.count(), 30)):
        locator = cards.nth(index)
        title = locator.inner_text().strip().replace("\n", " ")
        href = locator.get_attribute("href") or ""
        if not title or title in seen:
            continue
        seen.add(title)
        score = max(100 - index, 1) * 1000
        items.append({
            "id": href or f"zhihu-{index + 1}",
            "title": title,
            "summary": "知乎热榜页面抓取",
            "score": score,
        })
    return items


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--platform", required=True)
    parser.add_argument("--profile-dir", default="")
    args = parser.parse_args()

    with sync_playwright() as p:
        if args.platform == "zhihu" and args.profile_dir:
            profile_dir = Path(args.profile_dir).expanduser().resolve()
            profile_dir.mkdir(parents=True, exist_ok=True)
            context = p.chromium.launch_persistent_context(
                user_data_dir=str(profile_dir),
                headless=True,
                viewport={"width": 1440, "height": 1600},
            )
            page = context.pages[0] if context.pages else context.new_page()
            result = fetch_zhihu(page, profile_dir)
            context.close()
        else:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page(viewport={"width": 1440, "height": 1600})
            if args.platform == "weibo":
                result = fetch_weibo(page)
            else:
                result = []
            browser.close()
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
