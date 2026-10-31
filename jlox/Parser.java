package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

// 语法分析器
/// 把 Scanner 类生成的 Token 流转化为AST抽象语法树
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
    // expression -> assignment ;
    return assignment();
  }

  // 语句解析
  private Stmt statement() {
    // For loop
    if(match(FOR)) return forStatement();
    // if statment
    if(match(IF)) return ifStatement();
    // 打印语句
    if(match(PRINT)) return printStatement();
    if(match(RETURN)) return returnStatement();
    // while loop
    if(match(WHILE)) return whileStatement();
    // '{'
    if(match(LEFT_BRACE)) return new Stmt.Block(block());
    // 表达式语句
    return expressionStatement();
  }

  /// forStmt -> "for" "(" ( varDecl | exprStmt | ";") expression? ";" expression? ")" statement ;
  /// **脱糖**, 这里没有独立的实现一个新的语句, 而是把for脱糖成为了while语句.
  private Stmt forStatement(){
    // 消耗'('
    consume(LEFT_PAREN, "Expect '(' after 'for'.");
    /// 初始化语句 - 第一次循环开始前的执行语句
    Stmt initializer;
    if(match(SEMICOLON)) {
      // ';' 说明初始化语句是空的
      initializer = null;
    } else if (match(VAR)) {
      // var 开始说明定义了一个新的变量
      initializer = varDeclaration();
    } else {
      // 如果没有var开头, 说明这里是一个表达式
      /// 这是合法的, 因为这个位置的语义就是**首次循环开始之前执行一次**
      initializer = expressionStatement();
    }
    /// **此处不需要消耗';'** 因为前面当作了**语句**解析, ';'已经被消耗了

    /// 条件语句 - 每次循环执行之前执行的语句
    Expr condition = null;
    if(!check(SEMICOLON)) {
      // 只要不是';'说明这里有一个条件语句
      condition = expression();
    }
    consume(SEMICOLON, "Expect ';' after loop condition.");

    /// 终止语句 - 每次循环之前执行的语句
    Expr increment = null;
    if(!check(RIGHT_PAREN)) {
      // 只要不是')'就执行说明这是一个表达式
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");

    // 函数体 while 是statement的子类型
    Stmt body = statement();

    /// **倒置处理**

    // push 入 后置语句
    if(increment != null) {
      body = new Stmt.Block(
          Arrays.asList(
            body,
            new Stmt.Expression(increment)
            )
          );
    }

    // 空语句恒为真
    if(condition == null) condition = new Expr.Literal(true);
    // 实现while类型
    body = new Stmt.While(condition, body);

    // 如果有初始化语句, 那么就作为一个statement语句返回回去
    if(initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }

  // ifStmt -> "if" "(" expression ")" statement ( "else" statement)? ;
  private Stmt ifStatement(){
    // if(condition)
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition.");

    // {thenBranch} else {elseBranch}
    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if(match(ELSE)){
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  // block -> "{" expression* "}" ;
  private List<Stmt> block(){
    List<Stmt> statements = new ArrayList<>();

    // 在调用block函数的地方'{'已经被消耗, 所以这里检查'}'.
    while(!check(RIGHT_BRACE) && !isAtEnd()){
      // 只要不是, 我们就解析一次定义语句
      statements.add(declaration());
    }

    // 消耗'}'
    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }

  // 解析打印语句
  // printStmt  -> "print" expression ";" ;
  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  private Stmt returnStatement(){
    Token keyword = previous();
    Expr value = null;
    if(!check(SEMICOLON)){
      value = expression();
    }
    consume(SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }

  // 解析定义变量语句
  // varDecl     -> "var" IDENTIFIER ( "=" expression )? ";" ;
  private Stmt varDeclaration() {
    // 需要一个标识符来作为变量的名字
    Token name = consume(IDENTIFIER, "Expect variable name.");

    // 默认是没有初始化语句
    Expr initializer = null;
    if(match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Except ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  // whileStmt       -> "while" "(" expression ")" statement ;
  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition.");
    Stmt body = statement();

    return new Stmt.While(condition, body);
  }

  // 解析表达式语句
  // exprStmt   -> expression ";" ;
  private Stmt expressionStatement(){
    Expr expr = expression();
    consume(SEMICOLON, "Except ';' after expression.");
    return new Stmt.Expression(expr);
  }

  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, "Expect " + kind + "name.");

    consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
    List<Token> parameters = new ArrayList<>();
    if(!check(RIGHT_PAREN)) {
      do{
        if(parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }
        parameters.add(
            consume(IDENTIFIER, "Expect parameter name."));
      } while(match(COMMA));
    }
    consume(RIGHT_PAREN, "Expect ')' after parameters.");

    consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
  }

  // 解析赋值语句
  // assignment  -> IDENTIFIER "=" assignment | equality ;
  private Expr assignment() {
    Expr expr = or();


    if(match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      // 如果表达式是一个变量的话, 那么强强转到变量类型, 并拿到他的name.
      if(expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  // or运算符
  // logic_or        -> logic_and ( "or" logic_and )* ;
  private Expr or() {
    Expr expr = and();

    // TODO hyw
    while(match(OR)){
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  // and 运算符
  // logic_and       -> equality ( "and" equality )* ;
  private Expr and(){
    Expr expr = equality();

    // TODO hyw
    while(match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
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

    return call();
  }

  private Expr call() {
    Expr expr = primary();

    while(true) {
      if(match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else {
        break;
      }
    }

    return expr;
  }

  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if(!check(RIGHT_PAREN)){
      do{
        if(arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
        arguments.add(expression());
      }while(match(COMMA));
    }

    Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
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

    // 如果是一个标识符的话, 说明这个位置是一个变量
    if(match(IDENTIFIER)) {
      return new Expr.Variable(previous());
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
      statements.add(declaration());
    }
    return statements;
  }

  // 解析声明
  private Stmt declaration() {
    try {
      if(match(FUN)) return function("function");
      // var 开头是一个变量声明
      if(match(VAR)) return varDeclaration();
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
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

  // 错误恢复
  // 当发生错误的时候, 执行这个方法, 把这个错误的语句丢弃掉.
  /// **恐慌模式**
  /// 丢弃所有不安全的字符, 直到开始一个新的语句
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
