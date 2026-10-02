package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

  private static final Interpreter interpreter = new Interpreter();

  // 错误标识, 在调用error函数输出错误的时候自动设置
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  // 主函数 根据参数判断执行模式
  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64); 
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  // 单个参数, 认为参数是文件名
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));

    if(hadError) System.exit(65);
    if(hadRuntimeError) System.exit(70);
  }

  // 没有参数, 交互式运行
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for(;;){
      System.out.print("> ");
      // Ctrl-D 发送 EOF
      // readLine() 函数返回 null
      String line = reader.readLine();
      if(line == null) break;
      run(line);
      // 每次成功运行, 都把错误设置为False
      hadError = false;
    }
  }

  // 脚本统一执行入口, 无论什么模式都是对这个函数的包装
  private static void run(String source){
    // 源码扫描为Token
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    // 解析Tokens到语句
    Parser parse = new Parser(tokens);
    List<Stmt> statements =  parse.parse();

    if(hadError) return;

    for(Token token : tokens){
      System.out.println("[Debug]: "+ token);
    }
    // System.out.println(new AstPrinter().print(expression));
    interpreter.interpret(statements);
  }

  // 触发错误, 供外部调用的函数
  static void error(int line, String message){
    report(line, "", message);
  }

  // 触发错误, 依据Token
  static void error(Token token, String message){
    if(token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    }else{
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() + 
        "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }

  // 报告错误, 私有函数, 内部使用.
  private static void report(int line, String where, String message){
    System.err.println(
        "[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }
}
