# M5 实战项目：文档生成演示
# 无 Docstring 的源代码（演示 Copilot 生成文档的过程）

from typing import Optional, List
from enum import Enum


class DiscountType(Enum):
    PERCENTAGE = "percentage"
    FIXED = "fixed"
    TIERED = "tiered"


class DiscountRule:
    def __init__(self, name: str, discount_type: DiscountType, value: float, min_amount: float = 0):
        self.name = name
        self.discount_type = discount_type
        self.value = value
        self.min_amount = min_amount
        self.is_active = True


class OrderItem:
    def __init__(self, product_id: int, name: str, price: float, quantity: int):
        self.product_id = product_id
        self.name = name
        self.price = price
        self.quantity = quantity

    def get_subtotal(self):
        return self.price * self.quantity


class Order:
    def __init__(self, order_id: int, customer_id: int):
        self.order_id = order_id
        self.customer_id = customer_id
        self.items: List[OrderItem] = []
        self.discount_rules: List[DiscountRule] = []
        self.tax_rate = 0.1

    def add_item(self, item: OrderItem):
        self.items.append(item)

    def get_subtotal(self):
        return sum(item.get_subtotal() for item in self.items)

    def apply_discount_rule(self, rule: DiscountRule):
        if rule.is_active and self.get_subtotal() >= rule.min_amount:
            self.discount_rules.append(rule)

    def calculate_discount_amount(self):
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
    def __init__(self, base_price: float, quantity: int = 1):
        self.base_price = base_price
        self.quantity = quantity
        self.modifiers: List[tuple] = []

    def apply_percentage_modifier(self, name: str, percentage: float):
        self.modifiers.append(('percentage', name, percentage))

    def apply_fixed_modifier(self, name: str, amount: float):
        self.modifiers.append(('fixed', name, amount))

    def calculate(self):
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
        result = self.calculate()
        return f"Final Price: {result['final_price']}\nSteps: {', '.join(result['calculation_steps'])}"


class VolumeDiscount:
    def __init__(self):
        self.tiers = []

    def add_tier(self, min_quantity: int, discount_percent: float):
        self.tiers.append({'min_qty': min_quantity, 'discount': discount_percent})
        self.tiers.sort(key=lambda x: x['min_qty'])

    def calculate_discount(self, unit_price: float, quantity: int):
        applicable_discount = 0

        for tier in self.tiers:
            if quantity >= tier['min_qty']:
                applicable_discount = tier['discount']

        discount_amount = unit_price * quantity * (applicable_discount / 100)
        final_price = unit_price * quantity - discount_amount

        return {
            'unit_price': unit_price,
            'quantity': quantity,
            'subtotal': unit_price * quantity,
            'discount_rate': applicable_discount,
            'discount_amount': round(discount_amount, 2),
            'final_price': round(final_price, 2)
        }

    def get_tier_summary(self):
        summary = "Volume Discount Tiers:\n"
        for tier in self.tiers:
            summary += f"  {tier['min_qty']}+ units: {tier['discount']}% off\n"
        return summary


def validate_discount_input(amount: float, rate: float) -> bool:
    return amount > 0 and 0 <= rate <= 100


def apply_seasonal_discount(price: float, season: str, base_discount: float) -> float:
    seasonal_multipliers = {
        'spring': 0.9,
        'summer': 0.85,
        'autumn': 0.95,
        'winter': 0.8
    }

    multiplier = seasonal_multipliers.get(season.lower(), 1.0)
    adjusted_price = price * multiplier
    final_price = adjusted_price * (1 - base_discount / 100)

    return round(final_price, 2)


def calculate_loyalty_discount(purchase_history: List[float], current_amount: float) -> float:
    if not purchase_history:
        return current_amount

    average_purchase = sum(purchase_history) / len(purchase_history)
    customer_tier = len(purchase_history) // 5

    discount_rate = min(customer_tier * 2, 15)
    discount_amount = current_amount * (discount_rate / 100)
    final_amount = current_amount - discount_amount

    return round(final_amount, 2)


if __name__ == '__main__':
    order = Order(1001, 5001)
    order.add_item(OrderItem(101, 'Laptop', 999.99, 1))
    order.add_item(OrderItem(102, 'Mouse', 25.50, 2))

    rule1 = DiscountRule('Holiday Sale', DiscountType.PERCENTAGE, 10, min_amount=500)
    order.apply_discount_rule(rule1)

    print("订单总额:")
    print(order.calculate_total())

    calc = PriceCalculator(100, 3)
    calc.apply_percentage_modifier('Spring Sale', -15)
    calc.apply_fixed_modifier('Loyalty Bonus', -10)
    print("\n价格计算:")
    print(calc.get_calculation_summary())

    volume = VolumeDiscount()
    volume.add_tier(1, 0)
    volume.add_tier(10, 5)
    volume.add_tier(50, 10)
    print("\n" + volume.get_tier_summary())
    print(volume.calculate_discount(50, 75))
