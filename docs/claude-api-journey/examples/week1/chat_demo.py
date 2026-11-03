import os
import sys

from anthropic import Anthropic

SYSTEM_PROMPT = "你是一位乐于助人的中文助手，回答简洁、准确、友好。"

HINTS = """用法：
- 直接输入文本对话（保留多轮上下文）
- /clear  清空对话历史
- /exit   退出"""


def main() -> None:
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        sys.exit("缺少 ANTHROPIC_API_KEY，请先 export ANTHROPIC_API_KEY=sk-ant-...")

    client = Anthropic(api_key=api_key)
    history: list[dict[str, str]] = []

    print(SYSTEM_PROMPT)
    print(HINTS)

    while True:
        try:
            user_input = input("\n你: ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break

        if user_input == "/exit":
            break
        if user_input == "/clear":
            history = []
            print("[已清空对话历史]")
            continue
        if not user_input:
            continue

        history.append({"role": "user", "content": user_input})

        print("Claude: ", end="", flush=True)
        with client.messages.stream(
            model="claude-sonnet-4-5",
            max_tokens=1024,
            system=SYSTEM_PROMPT,
            messages=history,
        ) as stream:
            reply = ""
            for text in stream.text_stream:
                print(text, end="", flush=True)
                reply += text
        print()

        history.append({"role": "assistant", "content": reply})


if __name__ == "__main__":
    main()
