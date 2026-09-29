package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
  // 原始文本
  private final String source;
  // 对应的Token列表
  private final List<Token> tokens = new ArrayList<>();

  // 开始坐标
  private int start = 0;
  // 当前解析位置
  private int current = 0;
  // 当前行号
  private int line = 1;

  // 保留字的映射表
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("class"  , CLASS);
    keywords.put("print"  , PRINT);
    keywords.put("return" , RETURN);
    keywords.put("super"  , SUPER);
    keywords.put("this"   , THIS);
    keywords.put("fun"    , FUN);

    keywords.put("true"   , TRUE);
    keywords.put("false"  , FALSE);

    keywords.put("var"    , VAR);
    keywords.put("nil"    , NIL);

    keywords.put("if"     , IF);
    keywords.put("else"   , ELSE);
    keywords.put("for"    , FOR);
    keywords.put("while"  , WHILE);

    keywords.put("and"    , AND);
    keywords.put("or"     , OR);
  }

  // 构造函数
  Scanner(String source){
    this.source = source;
  }

  // 扫描源码到Token
  List<Token> scanTokens(){
    while(!isAtEnd()){
      start = current;
      scanToken();
    }
    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  // 当前是否扫描到了源码的末尾
  private boolean isAtEnd(){
    return current >= source.length();
  }

  private void scanToken(){
    // 获取下一个字符
    char c = advance();
    switch(c){
      // 判断是不是单个操作符
      case '(': addToken(LEFT_PAREN);  break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE);  break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA);       break;
      case '.': addToken(DOT);         break;
      case '-': addToken(MINUS);       break;
      case '+': addToken(PLUS);        break;
      case ';': addToken(SEMICOLON);   break;
      case '*': addToken(STAR);        break;

    // 复合操作符 (!=, ==, <=, >=)
    // TODO 尚未理解match的作用
    // TODO: 解答, 这个问题体现在没有理解current变量和advance函数的作用
    // 对于advance函数而言, 他会读取当前的字符, 返回, 同时current指向下一个字符
    // 这意味着, current现在指向的字符是什么, 不知道.
    // match就是做这个的, 他来看看现在的字符是不是需要的? 
    // 如果是, 那么就步进(处理过了), 反之不步进
      case '!': 
        // 要么是不等于逻辑运算, 要么是逻辑反置
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=': 
        // 相等逻辑判断, 或者赋值运算符
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<': 
        // 小于等于或者小于
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>': 
        // 大于等于或者大于
        addToken(match('=') ? GREATER_EQUAL: GREATER);
        break;

      // 除法需要和//注释一起处理
      case '/':
        // 如果两个斜杠在一起, 那么只要后面不是换行符也不再文件末尾
        // 就都是注释, 所以可以一直步进
        if(match('/')){
          while(peek() != '\n' && !isAtEnd()) advance();
        }else if(match('*')){
          blockComment();
        }else{
          // 否则说明就是一个简单的除号
          addToken(SLASH);
        }
        break;
      // 空格, 回车符, 制表符都可以忽略不做
      case ' ':
      case '\r':
      case '\t':
        // Do nothing...
        break;
      // 换行符把行号增加即可
      case '\n':
        line++;
        break;
      // "表明接下来是一个字符串, 调用相应的函数解析
      case '"': string(); break;

      default:
        if(isDigit(c)){
          // 数字开头, 处理数字, 类似于字符串
          number();
        }else if(isAlpha(c)){
          // 如果是字母开头, 是不是关键字?
          identifier();
        }else {
          // 否则说明无法解析, 报错
          Lox.error(line, "Unexpected character.");
        }
        break;
    }
  }

  private void blockComment(){
    int startLine = line;
    while(!isAtEnd()){
      if(peek() == '*' && peekNext() == '/'){
        advance(); // '*'
        advance(); // '/'
        return;
      }
      if(peek() == '\n') line++;
      advance();
    }
    Lox.error(startLine, "Unterminated Block Comment.");
  }

  // 判断是不是保留字
  private void identifier(){
    // 如果是数字或者是字符就步进
    while(isAlphaNumberic(peek())) advance();

    // 把这部分读取出来
    String text = source.substring(start, current);
    // 拿到Map中去查, 看看能不能查到对应的类别
    TokenType type = keywords.get(text);
    // 如果每查到, 说明不是关键字
    if(type == null) type = IDENTIFIER;
    addToken(type);
  }

  // 是不是字母? (包含'_')
  private boolean isAlpha(char c){
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  // 是字母或者是数字?
  private boolean isAlphaNumberic(char c){
    return isAlpha(c) || isDigit(c);
  }

  // 是不是数字
  private boolean isDigit(char c){
    return c >= '0' && c <= '9';
  }

  // 捕获数字
  private void number(){
    // 只要当前位置仍然是数字, 就继续步进
    while(isDigit(peek())) advance();

    // 如果当前位置是'.', 并且后一个字符仍然是数字, 说明是小数
    if(peek() == '.' && isDigit(peekNext())) {
      advance();

      while(isDigit(peek())) advance();
    }
    // 构建一个Double类型数字
    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  // 返回下一个字符(不步进解析位置)
  private char peekNext(){
    if(current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }
  // 捕获字符串
  private void string(){
    // 只要接下来不在"上, 也不是文件的末尾, 就都可以步进
    while(peek() != '"' && !isAtEnd()){
      // lox 接受多行字符串
      if(peek() == '\n') line++;
      advance();
    }

    // 如果到了文件末尾, 就报错, 没有闭合的字符串
    if(isAtEnd()){
      Lox.error(line, "Unterminated string.");
      return;
    }

    // 步进, 到了这里, 说明处理完了字符串, 也不在文件末尾.
    advance();

    // TODO 不理解, 这里的Start就没有变过, 难道是一直在1开始? 这明显不合理
    // TODO: 答案是, 在scanTokens函数的开始, 把start设置成了current;
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }
  
  // 获取当前位置的字符
  private char peek(){
    if(isAtEnd()) return '\0';
    return source.charAt(current);
  }

  // 判断当前的字符是不是输入的字符
  private boolean match(char expected){
    if(isAtEnd()) return false;
    if(source.charAt(current) != expected) return false;

    // 到这里说明匹配了, 所以这个字符可以认为处理过了, 可以递增了
    current++;
    return true;
  }

  // 获取当前字符, 并递进current(后置自增)
  private char advance(){
    return source.charAt(current++);
  }

  // 封装, 函数重载, 部分函数不需要设置值, 所以可以使用这个函数
  // 对于需要的, 会调用下面的函数
  private void addToken(TokenType type){
    addToken(type, null);
  }

  // 加入一个Token到列表中
  private void addToken(TokenType type, Object literal){
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}

