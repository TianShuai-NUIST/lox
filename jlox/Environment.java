package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

// 作用域设置
class Environment {

  // 父作用域
  // 这个东西是递归包含的, 这意味着, 一个父作用域也会有一个父作用域, 这样我们就实现了层级化的变量屏蔽与包含
  final Environment enclosing;

  // 变量表
  // name --> values
  private final Map<String, Object> values = new HashMap<>();

  // 构造函数
  Environment(){
    enclosing = null;
  }

  // 包含父作用域的构造函数
  Environment(Environment enclosing){
    this.enclosing = enclosing;
  }

  // 获取变量值
  Object get(Token name) {
    // 首先检查当前作用域
    if(values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }

    // 递归检查上级作用域
    if(enclosing != null) return enclosing.get(name);

    // 抛出错误
    throw new RuntimeError(name, 
        "Undefined variable '" + name.lexeme + "'.");

    // 思路 --> 剥洋葱, 从内到外, 一层层看
  }

  // 变量赋值
  void assign(Token name, Object value){
    // 首先看当前作用域内有没有已经声明的同名变量
    if(values.containsKey(name.lexeme)) {
      // 有的话就赋值
      values.put(name.lexeme, value);
      return;
    }

    // 没有看看有没有上级作用域
    if(enclosing != null){
      // 有上级作用域, 递归赋值, 直到找到被声明的变量
      enclosing.assign(name, value);
      return;
    }

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }

  // 定义变量, 直接往当前作用域内写入变量的值
  void define(String name, Object value) {
    values.put(name, value);
  }
}
