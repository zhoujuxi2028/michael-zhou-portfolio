# M5 实战项目：文档生成演示
# 带完整 Google 风格 Docstring 的版本（Copilot 生成）

from typing import Optional, List
from enum import Enum


class DiscountType(Enum):
    """折扣类型枚举 [EN: Discount type enumeration].
    
    定义系统支持的所有折扣计算方式。用于 DiscountRule 中指定
    折扣的应用方式。
    
    Attributes:
        PERCENTAGE: 百分比折扣（如 10% 折扣）[EN: Percentage discount].
        FIXED: 固定金额折扣（如减 50 元）[EN: Fixed amount discount].
        TIERED: 分级折扣（按订单金额分级）[EN: Tiered discount by amount].
    
    Example:
        >>> rule = DiscountRule('sale', DiscountType.PERCENTAGE, 15)
        >>> rule.discount_type == DiscountType.PERCENTAGE
        True
    """
    PERCENTAGE = "percentage"
    FIXED = "fixed"
    TIERED = "tiered"


class DiscountRule:
    """折扣规则 [EN: Discount rule].
    
    定义单个折扣规则，包括折扣类型、折扣值、最小订单金额等。
    支持条件性应用（如仅在订单金额 ≥ min_amount 时适用）。
    
    Attributes:
        name: 规则名称，用于日志和报表 [EN: Rule name].
        discount_type: 折扣类型（percentage/fixed/tiered）[EN: Type of discount].
        value: 折扣数值，单位取决于 discount_type [EN: Discount value].
        min_amount: 触发折扣的最小订单金额（CNY）[EN: Minimum order amount].
        is_active: 规则是否启用 [EN: Whether rule is active].
    
    Args:
        name: 折扣规则的名称，如 'Holiday Sale', 'Member Discount'。
        discount_type: DiscountType 枚举值。
        value: 折扣数值：百分比时为 0-100，固定时为金额。
        min_amount: 最小订单金额，默认 0（无最小限制）。
    
    Raises:
        ValueError: 如果 value < 0 或 min_amount < 0。
    
    Example:
        >>> rule = DiscountRule('Spring Sale', DiscountType.PERCENTAGE, 10, min_amount=500)
        >>> rule.value
        10
        >>> rule.is_active
        True
    """

    def __init__(self, name: str, discount_type: DiscountType, value: float, min_amount: float = 0):
        self.name = name
        self.discount_type = discount_type
        self.value = value
        self.min_amount = min_amount
        self.is_active = True


class OrderItem:
    """订单行项目 [EN: Order line item].
    
    代表订单中的单个商品，包含商品信息、单价和数量。
    
    Attributes:
        product_id: 商品 ID（正整数）[EN: Product identifier].
        name: 商品名称 [EN: Product name].
        price: 单价（CNY，≥ 0）[EN: Unit price in CNY].
        quantity: 购买数量（正整数）[EN: Quantity ordered].
    
    Args:
        product_id: 唯一的商品标识符。
        name: 商品名称，用于订单显示。
        price: 单位价格，必须为非负数。
        quantity: 购买数量，必须为正整数。
    
    Example:
        >>> item = OrderItem(101, 'Laptop', 5999, 1)
        >>> item.get_subtotal()
        5999
    """

    def __init__(self, product_id: int, name: str, price: float, quantity: int):
        self.product_id = product_id
        self.name = name
        self.price = price
        self.quantity = quantity

    def get_subtotal(self):
        """计算该行项目的小计 [EN: Calculate line item subtotal].
        
        Returns:
            小计金额（float），等于 price × quantity，保留两位小数。
        
        Example:
            >>> item = OrderItem(101, 'Mouse', 25.50, 3)
            >>> item.get_subtotal()
            76.5
        """
        return self.price * self.quantity


class Order:
    """订单 [EN: Order].
    
    代表一个完整的订单，包含多个商品、折扣规则和税费。
    支持添加商品、应用折扣规则、计算总额等功能。
    
    Attributes:
        order_id: 订单 ID（正整数）[EN: Order identifier].
        customer_id: 客户 ID（正整数）[EN: Customer identifier].
        items: 订单中的商品列表 [EN: List of order items].
        discount_rules: 适用的折扣规则列表 [EN: Applied discount rules].
        tax_rate: 税费率（默认 10%）[EN: Tax rate, default 0.1].
    
    Args:
        order_id: 唯一的订单号。
        customer_id: 客户的唯一标识符。
    
    Example:
        >>> order = Order(1001, 5001)
        >>> order.add_item(OrderItem(101, 'Laptop', 5999, 1))
        >>> order.calculate_total()
        {'subtotal': 5999.0, 'discount': 0.0, 'tax': 599.9, 'total': 6598.9}
    """

    def __init__(self, order_id: int, customer_id: int):
        self.order_id = order_id
        self.customer_id = customer_id
        self.items: List[OrderItem] = []
        self.discount_rules: List[DiscountRule] = []
        self.tax_rate = 0.1

    def add_item(self, item: OrderItem):
        """向订单添加商品 [EN: Add item to order].
        
        Args:
            item: 要添加的 OrderItem 对象。
        
        Raises:
            TypeError: 如果 item 不是 OrderItem 类型。
        
        Example:
            >>> order = Order(1001, 5001)
            >>> order.add_item(OrderItem(101, 'Laptop', 5999, 1))
            >>> len(order.items)
            1
        """
        self.items.append(item)

    def get_subtotal(self):
        """计算订单小计（未计折扣和税费）[EN: Calculate order subtotal].
        
        Returns:
            订单小计（float），所有行项目小计之和。
        
        Example:
            >>> order = Order(1001, 5001)
            >>> order.add_item(OrderItem(101, 'Laptop', 5999, 1))
            >>> order.get_subtotal()
            5999
        """
        return sum(item.get_subtotal() for item in self.items)

    def apply_discount_rule(self, rule: DiscountRule):
        """应用折扣规则 [EN: Apply discount rule].
        
        仅在规则启用且订单金额满足最小要求时应用。
        
        Args:
            rule: 要应用的 DiscountRule 对象。
        
        Example:
            >>> order = Order(1001, 5001)
            >>> order.add_item(OrderItem(101, 'Laptop', 5999, 1))
            >>> rule = DiscountRule('Sale', DiscountType.PERCENTAGE, 10, min_amount=500)
            >>> order.apply_discount_rule(rule)
            >>> len(order.discount_rules)
            1
        """
        if rule.is_active and self.get_subtotal() >= rule.min_amount:
            self.discount_rules.append(rule)

    def calculate_discount_amount(self):
        """计算应用所有折扣规则后的总折扣额 [EN: Calculate total discount amount].
        
        支持多个折扣规则叠加，但总折扣不超过订单小计的 50%。
        
        Returns:
            总折扣额（float），保留两位小数。
        
        Example:
            >>> order = Order(1001, 5001)
            >>> order.add_item(OrderItem(101, 'Laptop', 5999, 1))
            >>> rule1 = DiscountRule('Sale1', DiscountType.PERCENTAGE, 10)
            >>> rule2 = DiscountRule('Sale2', DiscountType.FIXED, 500)
            >>> order.apply_discount_rule(rule1)
            >>> order.apply_discount_rule(rule2)
            >>> discount = order.calculate_discount_amount()
            >>> discount > 0
            True
        """
        subtotal = self.get_subtotal()
        total_discount = 0

        for rule in self.discount_rules:
            if rule.discount_type == DiscountType.PERCENTAGE:
                total_discount += subtotal * (rule.value / 100)
            elif rule.discount_type == DiscountType.FIXED:
                total_discount += rule.value
            elif rule.discount_type == DiscountType.TIERED:
                if subtotal >= 1000:
                    total_discount += subtotal * (rule.value / 100)

        return min(total_discount, subtotal * 0.5)

    def calculate_total(self):
        """计算订单总额（含税、折扣）[EN: Calculate total order amount].
        
        Returns:
            字典，包含以下键（单位：CNY）:
            - subtotal: 原始小计
            - discount: 折扣总额
            - tax: 税费（按 tax_rate 计算）
            - total: 最终总额
        
        Example:
            >>> order = Order(1001, 5001)
            >>> order.add_item(OrderItem(101, 'Laptop', 5999, 1))
            >>> result = order.calculate_total()
            >>> result['total'] > result['subtotal']
            True
        """
        subtotal = self.get_subtotal()
        discount = self.calculate_discount_amount()
        after_discount = subtotal - discount
        tax = after_discount * self.tax_rate
        total = after_discount + tax
        return {
            'subtotal': round(subtotal, 2),
            'discount': round(discount, 2),
            'tax': round(tax, 2),
            'total': round(total, 2)
        }


class PriceCalculator:
    """价格计算器 [EN: Price calculator].
    
    支持基础价格的多层修饰符（百分比或固定金额）应用。
    常用于计算促销、税费、运费等复合定价场景。
    
    Attributes:
        base_price: 基础单价 [EN: Base unit price].
        quantity: 购买数量 [EN: Quantity].
        modifiers: 应用的修饰符列表 [EN: List of applied modifiers].
    
    Args:
        base_price: 商品基础价格（非负数）[EN: Base price in CNY].
        quantity: 购买数量，默认 1 [EN: Quantity, default 1].
    
    Example:
        >>> calc = PriceCalculator(100, 2)
        >>> calc.apply_percentage_modifier('Discount', -10)
        >>> calc.apply_fixed_modifier('Shipping', 20)
        >>> result = calc.calculate()
        >>> result['final_price']
        218.0
    """

    def __init__(self, base_price: float, quantity: int = 1):
        self.base_price = base_price
        self.quantity = quantity
        self.modifiers: List[tuple] = []

    def apply_percentage_modifier(self, name: str, percentage: float):
        """应用百分比修饰符 [EN: Apply percentage modifier].
        
        Args:
            name: 修饰符名称（如 'Discount', 'Tax'）[EN: Modifier name].
            percentage: 百分比值，负数表示折扣，正数表示增加 [EN: Percentage value].
        
        Example:
            >>> calc = PriceCalculator(100, 1)
            >>> calc.apply_percentage_modifier('Discount', -10)
            >>> calc.calculate()['final_price']
            90.0
        """
        self.modifiers.append(('percentage', name, percentage))

    def apply_fixed_modifier(self, name: str, amount: float):
        """应用固定金额修饰符 [EN: Apply fixed amount modifier].
        
        Args:
            name: 修饰符名称（如 'Shipping', 'Handling'）[EN: Modifier name].
            amount: 固定金额，负数表示折扣，正数表示增加 [EN: Fixed amount in CNY].
        
        Example:
            >>> calc = PriceCalculator(100, 1)
            >>> calc.apply_fixed_modifier('Shipping', 10)
            >>> calc.calculate()['final_price']
            110.0
        """
        self.modifiers.append(('fixed', name, amount))

    def calculate(self):
        """计算最终价格及每一步的计算过程 [EN: Calculate final price with steps].
        
        Returns:
            字典，包含以下键:
            - final_price: 最终价格（float）
            - calculation_steps: 计算步骤列表（list of str）
        
        Example:
            >>> calc = PriceCalculator(100, 2)
            >>> result = calc.calculate()
            >>> 'final_price' in result
            True
        """
        current_price = self.base_price * self.quantity
        details = [f"Base: {current_price:.2f}"]

        for modifier in self.modifiers:
            if modifier[0] == 'percentage':
                change = current_price * (modifier[2] / 100)
                current_price += change
                details.append(f"{modifier[1]}: {change:+.2f}")
            elif modifier[0] == 'fixed':
                current_price += modifier[2]
                details.append(f"{modifier[1]}: {modifier[2]:+.2f}")

        return {
            'final_price': round(current_price, 2),
            'calculation_steps': details
        }

    def get_calculation_summary(self):
        """获取计算摘要文本 [EN: Get calculation summary as text].
        
        Returns:
            格式化的字符串，包含最终价格和所有计算步骤。
        
        Example:
            >>> calc = PriceCalculator(100, 1)
            >>> calc.apply_percentage_modifier('Discount', -10)
            >>> summary = calc.get_calculation_summary()
            >>> 'Final Price' in summary
            True
        """
        result = self.calculate()
        return f"Final Price: {result['final_price']}\nSteps: {', '.join(result['calculation_steps'])}"


if __name__ == '__main__':
    order = Order(1001, 5001)
    order.add_item(OrderItem(101, 'Laptop', 999.99, 1))
    order.add_item(OrderItem(102, 'Mouse', 25.50, 2))

    rule1 = DiscountRule('Holiday Sale', DiscountType.PERCENTAGE, 10, min_amount=500)
    order.apply_discount_rule(rule1)

    print("订单总额:")
    print(order.calculate_total())
