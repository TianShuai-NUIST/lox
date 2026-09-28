package com.craftinginterpreters.lox;

class Token{
  // TODO 现在还不知道所有的成员是做什么的.
  final TokenType type; // Token 的类型
  final String lexeme;
  final Object literal;
  final int line;       // Token 的行号

  // 构造函数, 初始化一个Token
  Token(TokenType type, String lexeme, Object literal, int line){
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  // 转化成String输出
  public String toString(){
    return type + " " + lexeme + " " + literal;
  }
}
