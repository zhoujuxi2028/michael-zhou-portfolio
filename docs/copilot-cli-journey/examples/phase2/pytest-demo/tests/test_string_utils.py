"""String utilities unit tests using Pytest.

这个文件包含 string_utils 模块的完整测试套件。
"""

import sys
from pathlib import Path

# 添加 src 到路径
sys.path.insert(0, str(Path(__file__).parent.parent))

import pytest
from src.string_utils import (
    reverse_string,
    is_palindrome,
    count_vowels,
    capitalize_words,
    remove_duplicates,
)


class TestReverseString:
    """reverse_string 函数的测试。"""
    
    def test_reverse_simple_string(self):
        """应该反转简单字符串。"""
        assert reverse_string("hello") == "olleh"
    
    def test_reverse_empty_string(self):
        """应该正确处理空字符串。"""
        assert reverse_string("") == ""
    
    def test_reverse_single_character(self):
        """应该正确处理单个字符。"""
        assert reverse_string("a") == "a"
    
    def test_reverse_with_invalid_input(self):
        """应该在输入不是字符串时抛出 TypeError。"""
        with pytest.raises(TypeError):
            reverse_string(123)


class TestIsPalindrome:
    """is_palindrome 函数的测试。"""
    
    def test_valid_palindrome(self):
        """应该识别回文字符串。"""
        assert is_palindrome("racecar") is True
    
    def test_non_palindrome(self):
        """应该识别非回文字符串。"""
        assert is_palindrome("hello") is False
    
    def test_palindrome_with_spaces(self):
        """应该忽略空格检查回文。"""
        assert is_palindrome("race car") is True
    
    def test_palindrome_with_invalid_input(self):
        """应该在输入不是字符串时抛出 TypeError。"""
        with pytest.raises(TypeError):
            is_palindrome(123)


class TestCountVowels:
    """count_vowels 函数的测试。"""
    
    def test_count_vowels_mixed(self):
        """应该计算混合字符串中的元音。"""
        assert count_vowels("hello") == 2
    
    def test_count_all_vowels(self):
        """应该计算包含所有元音的字符串。"""
        assert count_vowels("aeiou") == 5
    
    def test_count_no_vowels(self):
        """应该在没有元音时返回 0。"""
        assert count_vowels("xyz") == 0
    
    def test_count_vowels_invalid_input(self):
        """应该在输入不是字符串时抛出 TypeError。"""
        with pytest.raises(TypeError):
            count_vowels([])


class TestCapitalizeWords:
    """capitalize_words 函数的测试。"""
    
    def test_capitalize_multiple_words(self):
        """应该将多个单词的首字母大写。"""
        assert capitalize_words("hello world") == "Hello World"
    
    def test_capitalize_already_capitalized(self):
        """应该正确处理已大写的文本。"""
        assert capitalize_words("Hello") == "Hello"
    
    def test_capitalize_single_word(self):
        """应该将单个单词的首字母大写。"""
        assert capitalize_words("test") == "Test"
    
    def test_capitalize_invalid_input(self):
        """应该在输入不是字符串时抛出 TypeError。"""
        with pytest.raises(TypeError):
            capitalize_words(None)


class TestRemoveDuplicates:
    """remove_duplicates 函数的测试。"""
    
    def test_remove_all_duplicates(self):
        """应该移除所有重复字符。"""
        assert remove_duplicates("aabbcc") == "abc"
    
    def test_no_duplicates(self):
        """应该保留没有重复的字符串。"""
        assert remove_duplicates("abc") == "abc"
    
    def test_preserve_order(self):
        """应该保持原始字符顺序。"""
        assert remove_duplicates("aabaa") == "ab"
    
    def test_remove_duplicates_invalid_input(self):
        """应该在输入不是字符串时抛出 TypeError。"""
        with pytest.raises(TypeError):
            remove_duplicates({"a": 1})
