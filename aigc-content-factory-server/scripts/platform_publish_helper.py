import argparse
import json
import time
from datetime import datetime
from pathlib import Path

from playwright.sync_api import sync_playwright


SUCCESS_KEYWORDS = {
    "bilibili": ["投稿成功", "发布成功", "稿件投递成功", "查看稿件"],
    "douyin": ["发布成功", "作品管理", "查看作品"],
    "xiaohongshu": ["发布成功", "笔记发布成功", "查看笔记"],
}

UPLOAD_KEYWORDS = {
    "bilibili": ["上传视频", "稿件标题", "视频投稿", "立即投稿"],
    "douyin": ["上传视频", "作品发布", "发布作品", "添加作品描述"],
    "xiaohongshu": ["上传视频", "发布笔记", "填写标题", "输入标题"],
}


def write_state(path: Path, task_id: int, platform: str, status: str, message: str, platform_content_id: str = ""):
    payload = {
        "taskId": task_id,
        "platform": platform.upper(),
        "status": status,
        "message": message,
        "platformContentId": platform_content_id,
        "updatedAt": datetime.now().isoformat(),
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def body_text(page) -> str:
    try:
        return page.locator("body").inner_text(timeout=5000)
    except Exception:
        return ""


def any_visible(page, selectors):
    for selector in selectors:
        locator = page.locator(selector)
        if locator.count() > 0 and locator.first.is_visible():
            return locator.first
    return None


def click_text_if_exists(page, text):
    try:
        locator = page.get_by_text(text, exact=False)
        if locator.count() > 0 and locator.first.is_visible():
            locator.first.click(timeout=2000)
            return True
    except Exception:
        pass
    return False


def is_upload_ready(page, platform: str) -> bool:
    if page.locator("input[type='file']").count() > 0:
        return True
    text = body_text(page)
    return any(keyword in text for keyword in UPLOAD_KEYWORDS[platform])


def wait_for_login_and_upload(page, platform: str, timeout_sec: int):
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        if is_upload_ready(page, platform):
            return
        if platform == "douyin":
            click_text_if_exists(page, "扫码登录")
        if platform == "bilibili":
            click_text_if_exists(page, "扫码登录")
        page.wait_for_timeout(1000)
    raise RuntimeError("等待登录并进入上传页超时")


def set_video(page, video_path: str):
    file_input = page.locator("input[type='file']").first
    if file_input.count() == 0:
        raise RuntimeError("未找到视频上传控件")
    file_input.set_input_files(video_path)
    page.wait_for_timeout(3000)


def fill_text_input(locator, value: str):
    locator.click()
    locator.fill("")
    locator.fill(value)


def fill_editor(page, description: str):
    selectors = [
        "textarea[placeholder*='简介']",
        "textarea[placeholder*='描述']",
        "textarea[placeholder*='正文']",
        "textarea[placeholder*='内容']",
        "[contenteditable='true'][placeholder*='简介']",
        "[contenteditable='true'][data-placeholder*='简介']",
        "[contenteditable='true'][placeholder*='描述']",
        "[contenteditable='true'][data-placeholder*='描述']",
        "textarea",
        "[contenteditable='true']",
    ]
    locator = any_visible(page, selectors)
    if locator is None:
        return False
    try:
        tag_name = locator.evaluate("el => el.tagName.toLowerCase()")
        locator.click()
        if tag_name == "textarea":
            locator.fill(description)
        else:
            page.keyboard.press("Meta+A")
            page.keyboard.insert_text(description)
        return True
    except Exception:
        return False


def fill_fields(page, platform: str, title: str, description: str):
    title_selectors = [
        "input[placeholder*='标题']",
        "input[placeholder*='请输入标题']",
        "input[placeholder*='稿件']",
        "input[placeholder*='作品标题']",
        "input[aria-label*='标题']",
    ]
    title_input = any_visible(page, title_selectors)
    if title_input:
        fill_text_input(title_input, title)
    desc_filled = fill_editor(page, description)
    return bool(title_input), desc_filled


def detect_publish_success(page, platform: str):
    text = body_text(page)
    if any(keyword in text for keyword in SUCCESS_KEYWORDS[platform]):
        return True, text[:1000]
    return False, text[:1000]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--platform", required=True)
    parser.add_argument("--upload-url", required=True)
    parser.add_argument("--video-path", required=True)
    parser.add_argument("--title", required=True)
    parser.add_argument("--description", required=True)
    parser.add_argument("--profile-dir", required=True)
    parser.add_argument("--timeout-sec", type=int, default=180)
    parser.add_argument("--status-file", required=True)
    args = parser.parse_args()

    platform = args.platform.lower()
    task_id = int(Path(args.status_file).stem.split("-")[-1])
    status_file = Path(args.status_file).expanduser().resolve()
    profile_dir = Path(args.profile_dir).expanduser().resolve()
    profile_dir.mkdir(parents=True, exist_ok=True)

    with sync_playwright() as p:
        context = p.chromium.launch_persistent_context(
            user_data_dir=str(profile_dir),
            headless=False,
            viewport={"width": 1440, "height": 1100},
            args=["--disable-blink-features=AutomationControlled"],
        )
        page = context.pages[0] if context.pages else context.new_page()
        page.goto(args.upload_url, wait_until="domcontentloaded", timeout=60000)
        page.bring_to_front()

        write_state(status_file, task_id, platform, "MANUAL_REQUIRED", "等待用户登录")
        wait_for_login_and_upload(page, platform, args.timeout_sec)

        set_video(page, args.video_path)
        title_filled, desc_filled = fill_fields(page, platform, args.title, args.description)
        write_state(
            status_file,
            task_id,
            platform,
            "MANUAL_REQUIRED",
            f"已完成自动填充，等待用户手动点击发布。title_filled={title_filled}, description_filled={desc_filled}",
        )

        for _ in range(1800):
            ok, message = detect_publish_success(page, platform)
            if ok:
                write_state(status_file, task_id, platform, "SUCCESS", message[:1000])
                page.wait_for_timeout(3000)
                context.close()
                return
            if not context.pages or all(item.is_closed() for item in context.pages):
                write_state(status_file, task_id, platform, "FAILED", "浏览器已关闭，未检测到发布成功")
                return
            time.sleep(1)

        write_state(status_file, task_id, platform, "FAILED", "等待发布成功超时")
        context.close()


if __name__ == "__main__":
    main()
