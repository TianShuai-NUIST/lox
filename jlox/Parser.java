package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

// 语法分析器
class Parser {
  // 运行时语法错误
  private static class ParseError extends RuntimeException {};

  private final List<Token> tokens;
  private int current = 0;

  // 构造函数
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  // 表达式解析
  private Expr expression(){
    // expression -> equality ;
    return equality();
  }

  // 语句解析
  private Stmt statement() {
    // 打印语句
    if(match(PRINT)) return printStatement();
    // 表达式语句
    return expressionStatement();
  }

  // 解析打印语句
  // printStmt  -> "print" expression ";" ;
  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  // 解析表达式语句
  // exprStmt   -> expression ";" ;
  private Stmt expressionStatement(){
    Expr expr = expression();
    consume(SEMICOLON, "Except ';' after expression.");
    return new Stmt.Expression(expr);
  }

  // equality -> comparison ( ( "!=" | "==" ) comparison )*;
  private Expr equality() {
    Expr expr = comparison();

    while(match(BANG_EQUAL, EQUAL_EQUAL)) {
      // match 已经步进了, 所以需要获取前面一个Token
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  // comparison -> term ( (">" | ">=" | "<" | "<=" ) term )*;
  private Expr comparison(){
    Expr expr = term();

    // 比较运算符
    while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  // term       -> factor ( ( "-" | "+" ) factor )* ;
  private Expr term() {
    Expr expr = factor();

    // + 或者 -
    while(match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  // factor     -> unary ( ( "/" | "*" ) unary )* ;
  private Expr factor() {
    Expr expr = unary();
    // * 或者 / 
    while(match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  // unary      -> ( "!" | "-" ) unary | primary ;
  private Expr unary() {
    // 一元运算符一定是两个操作符和一个低级
    if(match(BANG, MINUS)){
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  // primary    -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
  private Expr primary() {
    if(match(FALSE)) return new Expr.Literal(false);
    if(match(TRUE))  return new Expr.Literal(true);
    if(match(NIL))   return new Expr.Literal(null);

    // 字面量
    if(match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    // '(' 说明是一个括号构成的表达式组
    if(match(LEFT_PAREN)) {
      // 解析表达式
      Expr expr = expression();
      // 如果没有找到")"就报错
      consume(RIGHT_PAREN, "Expect ')' after expressoin.");
      // 返回一个Grouping.
      return new Expr.Grouping(expr);
    }

    // 报错
    throw error(peek(), "Expect expression.");
  }
 
  // 解析Tokens 返回语句
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while(!isAtEnd()) {
      statements.add(statement());
    }
    return statements;
  }

  // 消耗一个Token (步进)
  private Token consume(TokenType type, String message){
    if(check(type)) return advance();

    // 如果不是, 就报错
    throw error(peek(), message);
  }

  // 报错函数
  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    // Tips: 技巧, 上面使用throw抛出了这个错误, 标准的写法应该是throw new ParseError();
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while(!isAtEnd()){
      if(previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

  // 步进 参数列表个数 步, 如果匹配任意元素, 则返回true
  private boolean match(TokenType ... types){
    for(TokenType type : types){
      if(check(type)){
        advance();
        return true;
      }
    }
    return false;
  }

  // 判断当前的Token的类型是不是所提供的类型
  private boolean check(TokenType type){
    if(isAtEnd()) return false;
    return peek().type == type;
  }

  // 步进1, 并返回前一个Token
  private Token advance(){
    if(!isAtEnd()) current++;
    return previous();
  }

  // 判断是不是到了Token列表的最后
  private boolean isAtEnd(){
    return peek().type == EOF;
  }

  // 获取当前的Token
  private Token peek(){
    return tokens.get(current);
  }

  // 获取前一个Token
  private Token previous() {
    return tokens.get(current - 1);
  }

}
