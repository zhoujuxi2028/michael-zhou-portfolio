"""字符串处理工具库。

包含常用的字符串操作函数，供测试学习使用。
"""


def reverse_string(s: str) -> str:
    """反转字符串。
    
    Args:
        s: 输入字符串
        
    Returns:
        反转后的字符串
        
    Raises:
        TypeError: 如果输入不是字符串
    """
    if not isinstance(s, str):
        raise TypeError("输入必须是字符串")
    return s[::-1]


def is_palindrome(s: str) -> bool:
    """检查字符串是否为回文。
    
    忽略空格和大小写。
    
    Args:
        s: 输入字符串
        
    Returns:
        如果是回文则为 True，否则为 False
        
    Raises:
        TypeError: 如果输入不是字符串
    """
    if not isinstance(s, str):
        raise TypeError("输入必须是字符串")
    
    cleaned = s.replace(" ", "").lower()
    return cleaned == cleaned[::-1]


def count_vowels(s: str) -> int:
    """计算字符串中的元音字母数量。
    
    统计 a, e, i, o, u (大小写)
    
    Args:
        s: 输入字符串
        
    Returns:
        元音字母的数量
        
    Raises:
        TypeError: 如果输入不是字符串
    """
    if not isinstance(s, str):
        raise TypeError("输入必须是字符串")
    
    vowels = "aeiouAEIOU"
    return sum(1 for char in s if char in vowels)


def capitalize_words(s: str) -> str:
    """将字符串中每个单词的首字母大写。
    
    Args:
        s: 输入字符串
        
    Returns:
        首字母大写后的字符串
        
    Raises:
        TypeError: 如果输入不是字符串
    """
    if not isinstance(s, str):
        raise TypeError("输入必须是字符串")
    
    return " ".join(word.capitalize() for word in s.split())


def remove_duplicates(s: str) -> str:
    """移除字符串中的重复字符，保持原始顺序。
    
    Args:
        s: 输入字符串
        
    Returns:
        移除重复字符后的字符串
        
    Raises:
        TypeError: 如果输入不是字符串
    """
    if not isinstance(s, str):
        raise TypeError("输入必须是字符串")
    
    seen = set()
    result = []
    for char in s:
        if char not in seen:
            seen.add(char)
            result.append(char)
    return "".join(result)
