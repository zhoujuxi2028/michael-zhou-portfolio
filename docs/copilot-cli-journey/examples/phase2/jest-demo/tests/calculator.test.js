const { add, subtract, multiply, divide, power } = require('../src/calculator.js');

describe('Calculator Library', () => {
  describe('add function', () => {
    it('should return 5 when adding 2 and 3', () => {
      expect(add(2, 3)).toBe(5);
    });

    it('should return -2 when adding -5 and 3', () => {
      expect(add(-5, 3)).toBe(-2);
    });

    it('should return 0 when adding 0 and 0', () => {
      expect(add(0, 0)).toBe(0);
    });

    it('should return 6 when adding 2.5 and 3.5', () => {
      expect(add(2.5, 3.5)).toBeCloseTo(6, 5);
    });

    it('should throw TypeError when first argument is not a number', () => {
      expect(() => add('a', 2)).toThrow(TypeError);
    });
  });

  describe('subtract function', () => {
    it('should return 2 when subtracting 3 from 5', () => {
      expect(subtract(5, 3)).toBe(2);
    });

    it('should return -2 when subtracting 5 from 3', () => {
      expect(subtract(3, 5)).toBe(-2);
    });

    it('should return 0 when subtracting a number from itself', () => {
      expect(subtract(5, 5)).toBe(0);
    });

    it('should throw TypeError when second argument is not a number', () => {
      expect(() => subtract(5, 'a')).toThrow(TypeError);
    });
  });

  describe('multiply function', () => {
    it('should return 12 when multiplying 3 by 4', () => {
      expect(multiply(3, 4)).toBe(12);
    });

    it('should return 0 when multiplying by 0', () => {
      expect(multiply(5, 0)).toBe(0);
    });

    it('should return 10 when multiplying 2.5 by 4', () => {
      expect(multiply(2.5, 4)).toBeCloseTo(10, 5);
    });

    it('should throw TypeError when first argument is not a number', () => {
      expect(() => multiply('x', 2)).toThrow(TypeError);
    });
  });

  describe('divide function', () => {
    it('should return 5 when dividing 10 by 2', () => {
      expect(divide(10, 2)).toBe(5);
    });

    it('should return 3.5 when dividing 7 by 2', () => {
      expect(divide(7, 2)).toBeCloseTo(3.5, 5);
    });

    it('should throw Error when dividing by 0', () => {
      expect(() => divide(5, 0)).toThrow(Error);
    });

    it('should throw TypeError when divisor is not a number', () => {
      expect(() => divide(5, 'a')).toThrow(TypeError);
    });
  });

  describe('power function', () => {
    it('should return 8 when raising 2 to power 3', () => {
      expect(power(2, 3)).toBe(8);
    });

    it('should return 1 when raising any number to power 0', () => {
      expect(power(5, 0)).toBe(1);
    });

    it('should return 0 when raising 0 to any positive power', () => {
      expect(power(0, 5)).toBe(0);
    });

    it('should throw Error when exponent is negative', () => {
      expect(() => power(2, -1)).toThrow(Error);
    });
  });
});
