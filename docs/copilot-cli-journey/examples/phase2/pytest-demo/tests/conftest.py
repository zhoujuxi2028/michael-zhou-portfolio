"""Pytest 配置和共享 fixtures。"""

import pytest


@pytest.fixture
def sample_string():
    """提供简单的测试字符串。"""
    return "hello"


@pytest.fixture
def palindrome_string():
    """提供回文字符串。"""
    return "racecar"


@pytest.fixture
def mixed_case_string():
    """提供大小写混合的字符串。"""
    return "Hello World"


@pytest.fixture
def duplicate_string():
    """提供含有重复字符的字符串。"""
    return "aabbcc"
