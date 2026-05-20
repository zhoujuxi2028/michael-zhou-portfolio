# M5 实战项目：基础使用示例

# 导入文档化版本的模块
import sys
sys.path.insert(0, '../src')

from discount_engine_documented import (
    Order, OrderItem, DiscountRule, DiscountType,
    PriceCalculator, VolumeDiscount
)


def example_1_simple_order():
    """示例 1: 简单订单计算"""
    print("=" * 60)
    print("示例 1: 简单订单计算（无折扣）")
    print("=" * 60)
    
    order = Order(1001, 5001)
    order.add_item(OrderItem(101, 'Laptop', 5999, 1))
    order.add_item(OrderItem(102, 'Mouse', 25.50, 2))
    
    result = order.calculate_total()
    print(f"商品小计: {result['subtotal']} CNY")
    print(f"折扣额: {result['discount']} CNY")
    print(f"税费: {result['tax']} CNY")
    print(f"总金额: {result['total']} CNY")
    print()


def example_2_percentage_discount():
    """示例 2: 百分比折扣"""
    print("=" * 60)
    print("示例 2: 应用百分比折扣")
    print("=" * 60)
    
    order = Order(1002, 5002)
    order.add_item(OrderItem(101, 'Laptop', 5999, 1))
    order.add_item(OrderItem(102, 'Mouse', 25.50, 2))
    
    rule = DiscountRule('Holiday Sale', DiscountType.PERCENTAGE, 10, min_amount=500)
    order.apply_discount_rule(rule)
    
    result = order.calculate_total()
    print(f"商品小计: {result['subtotal']} CNY")
    print(f"折扣额: {result['discount']} CNY（10%）")
    print(f"税费: {result['tax']} CNY")
    print(f"总金额: {result['total']} CNY")
    print()


def example_5_volume_discount():
    """示例 5: 分级优惠"""
    print("=" * 60)
    print("示例 5: 分级优惠（按购买量）")
    print("=" * 60)
    
    volume_discount = VolumeDiscount()
    volume_discount.add_tier(1, 0)
    volume_discount.add_tier(10, 5)
    volume_discount.add_tier(50, 10)
    
    print(volume_discount.get_tier_summary())
    
    for qty in [5, 15, 75]:
        result = volume_discount.calculate_discount(50, qty)
        print(f"购买 {qty} 件 @ 50 CNY/件: {result['final_price']} CNY")
    print()


if __name__ == '__main__':
    print("\nM5 Python 文档生成演示项目 - 基础使用示例\n")
    example_1_simple_order()
    example_2_percentage_discount()
    example_5_volume_discount()
