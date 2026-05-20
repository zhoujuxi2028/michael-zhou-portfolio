/**
 * 加法函数
 * @param {number} a - 第一个数
 * @param {number} b - 第二个数
 * @returns {number} 两数之和
 */
function add(a, b) {
  if (typeof a !== 'number' || typeof b !== 'number') {
    throw new TypeError('参数必须是数字');
  }
  return a + b;
}

/**
 * 减法函数
 * @param {number} a - 被减数
 * @param {number} b - 减数
 * @returns {number} 两数之差
 */
function subtract(a, b) {
  if (typeof a !== 'number' || typeof b !== 'number') {
    throw new TypeError('参数必须是数字');
  }
  return a - b;
}

/**
 * 乘法函数
 * @param {number} a - 第一个数
 * @param {number} b - 第二个数
 * @returns {number} 两数之积
 */
function multiply(a, b) {
  if (typeof a !== 'number' || typeof b !== 'number') {
    throw new TypeError('参数必须是数字');
  }
  return a * b;
}

/**
 * 除法函数
 * @param {number} a - 被除数
 * @param {number} b - 除数
 * @returns {number} 两数之商
 * @throws {Error} 当除数为0时抛出错误
 */
function divide(a, b) {
  if (typeof a !== 'number' || typeof b !== 'number') {
    throw new TypeError('参数必须是数字');
  }
  if (b === 0) {
    throw new Error('除数不能为0');
  }
  return a / b;
}

/**
 * 乘方函数
 * @param {number} base - 底数
 * @param {number} exponent - 指数
 * @returns {number} base的exponent次方
 * @throws {Error} 当指数为负数时抛出错误
 */
function power(base, exponent) {
  if (typeof base !== 'number' || typeof exponent !== 'number') {
    throw new TypeError('参数必须是数字');
  }
  if (exponent < 0) {
    throw new Error('指数不能为负数');
  }
  return Math.pow(base, exponent);
}

module.exports = { add, subtract, multiply, divide, power };
