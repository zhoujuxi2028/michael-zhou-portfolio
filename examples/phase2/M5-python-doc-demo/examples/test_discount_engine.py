# M5 实战项目：Pytest 单元测试示例

import sys
sys.path.insert(0, '../src')
import pytest

from discount_engine_documented import (
    Order, OrderItem, DiscountRule, DiscountType, PriceCalculator
)


class TestOrderItem:
    def test_subtotal(self):
        from discount_engine_documented import OrderItem
        item = OrderItem(101, 'Mouse', 25.50, 3)
        assert item.get_subtotal() == 76.50


class TestOrder:
    def test_add_item(self):
        order = Order(1001, 5001)
        order.add_item(OrderItem(101, 'Laptop', 5999, 1))
        assert len(order.items) == 1
    
    def test_discount_percentage(self):
        order = Order(1001, 5001)
        order.add_item(OrderItem(101, 'Laptop', 5999, 1))
        rule = DiscountRule('Sale', DiscountType.PERCENTAGE, 10, min_amount=500)
        order.apply_discount_rule(rule)
        discount = order.calculate_discount_amount()
        assert discount == pytest.approx(599.9, 0.01)


class TestPriceCalculator:
    def test_percentage_modifier(self):
        calc = PriceCalculator(100, 1)
        calc.apply_percentage_modifier('Discount', -10)
        result = calc.calculate()
        assert result['final_price'] == 90
