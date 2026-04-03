import argparse
import sys
import time
from pathlib import Path

from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeoutError


def page_text(page) -> str:
    try:
        return page.locator("body").inner_text(timeout=5000)
    except Exception:
        return ""


def is_login_page(page) -> bool:
    text = page_text(page)
    return any(keyword in text for keyword in ["扫码登录", "账号登录", "密码登录", "短信登录"])


def click_if_visible(page, selector: str) -> bool:
    locator = page.locator(selector)
    if locator.count() > 0 and locator.first.is_visible():
        locator.first.click()
        return True
    return False


def wait_until_upload_ready(page, timeout_sec: int) -> None:
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        if page.locator("input[type='file']").count() > 0:
            return
        text = page_text(page)
        if any(keyword in text for keyword in ["上传视频", "稿件标题", "立即投稿", "视频投稿"]):
            return
        if is_login_page(page):
            click_if_visible(page, "text=扫码登录")
            click_if_visible(page, "text=使用手机")
        page.wait_for_timeout(1000)
    raise RuntimeError("等待 B站上传页超时，请确认是否已扫码登录")


def fill_title(page, title: str) -> bool:
    selectors = [
        "input[placeholder*='标题']",
        "input[placeholder*='稿件']",
        "input[aria-label*='标题']",
        "input[aria-label*='稿件']",
    ]
    for selector in selectors:
        locator = page.locator(selector)
        if locator.count() > 0:
            locator.first.click()
            locator.first.fill(title)
            return True
    return False


def fill_description(page, description: str) -> bool:
    selectors = [
        "textarea[placeholder*='简介']",
        "textarea[placeholder*='描述']",
        "[contenteditable='true'][data-placeholder*='简介']",
        "[contenteditable='true'][placeholder*='简介']",
        "[contenteditable='true']",
        "textarea",
    ]
    for selector in selectors:
        locator = page.locator(selector)
        if locator.count() > 0:
            try:
                target = locator.first
                target.click()
                if target.evaluate("el => el.tagName.toLowerCase()") == "textarea":
                    target.fill(description)
                else:
                    page.keyboard.press("Meta+A")
                    page.keyboard.insert_text(description)
                return True
            except Exception:
                continue
    return False


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--upload-url", required=True)
    parser.add_argument("--video-path", required=True)
    parser.add_argument("--title", required=True)
    parser.add_argument("--description", required=True)
    parser.add_argument("--profile-dir", required=True)
    parser.add_argument("--timeout-sec", type=int, default=180)
    args = parser.parse_args()

    video_path = Path(args.video_path).expanduser().resolve()
    profile_dir = Path(args.profile_dir).expanduser().resolve()
    profile_dir.mkdir(parents=True, exist_ok=True)

    with sync_playwright() as playwright:
        context = playwright.chromium.launch_persistent_context(
            user_data_dir=str(profile_dir),
            headless=False,
            viewport={"width": 1440, "height": 1100},
            args=["--disable-blink-features=AutomationControlled"],
        )
        page = context.pages[0] if context.pages else context.new_page()
        page.goto(args.upload_url, wait_until="domcontentloaded", timeout=60000)
        page.bring_to_front()
        wait_until_upload_ready(page, args.timeout_sec)

        file_input = page.locator("input[type='file']").first
        if file_input.count() == 0:
            raise RuntimeError("未找到视频上传控件")

        file_input.set_input_files(str(video_path))
        page.wait_for_timeout(3000)
        fill_title(page, args.title)
        page.wait_for_timeout(1000)
        fill_description(page, args.description)
        page.wait_for_timeout(1000)

        print("BILIBILI_HELPER_READY")
        print(f"video={video_path}")
        print("title_filled=true")
        print("description_filled=true")
        print("等待用户手动点击发布并关闭浏览器...")

        while True:
            if not context.pages:
                break
            if all(page_item.is_closed() for page_item in context.pages):
                break
            time.sleep(1)

        context.close()
        return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except PlaywrightTimeoutError as exception:
        print(f"TIMEOUT: {exception}", file=sys.stderr)
        sys.exit(1)
    except Exception as exception:
        print(f"ERROR: {exception}", file=sys.stderr)
        sys.exit(1)
