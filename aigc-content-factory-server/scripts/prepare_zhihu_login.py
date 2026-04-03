from pathlib import Path

from playwright.sync_api import sync_playwright


def main():
    profile_dir = Path("./runtime/browser/zhihu").resolve()
    profile_dir.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        context = p.chromium.launch_persistent_context(
            user_data_dir=str(profile_dir),
            headless=False,
            viewport={"width": 1440, "height": 1600},
        )
        page = context.pages[0] if context.pages else context.new_page()
        page.goto("https://www.zhihu.com/hot", wait_until="domcontentloaded", timeout=60000)
        print("请在打开的浏览器中完成知乎登录，登录后关闭浏览器窗口。")
        while context.pages and not all(item.is_closed() for item in context.pages):
            page.wait_for_timeout(1000)
        context.close()


if __name__ == "__main__":
    main()
