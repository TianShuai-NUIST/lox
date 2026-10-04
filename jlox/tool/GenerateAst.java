package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

// 生成抽象语法树文件
public class GenerateAst{
  // 允许用户设定输出的目录
  public static void main(String[] args) throws IOException {
    if(args.length != 1){
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];

    // 定义抽象语法树
    // Arrays.asList()函数生成一个列表
    // 生成Expr.java文件
    defineAst(outputDir, "Expr", Arrays.asList(
          "Assign   : Token name, Expr value",                // 赋值表达式
          "Binary   : Expr left, Token operator, Expr right", // 二元操作符
          "Grouping : Expr expression",                       // 表达式组
          "Literal  : Object value",                          // 字面量
          "Unary    : Token operator, Expr right",            // 一元操作符
          "Variable : Token name"                             // 声明变量
          ));

    // 生成Stmt.java文件
    defineAst(outputDir, "Stmt", Arrays.asList(
          "Block      : List<Stmt> statements",       // 语句块
          "Expression : Expr expression",             // 表达式语句
          "Print      : Expr expression",             // 打印语句
          "Var        : Token name, Expr initializer" // 变量定义语句
          ));
  }

  // 生成一个抽象语法树
  private static void defineAst(
    String outputDir, String baseName, List<String> types) throws IOException {

    String path = outputDir + '/' + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    // 元信息设置
    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    // 定义抽象类, 要求子类必须实现内部函数(abstract)
    writer.println("abstract class " + baseName + "{");
    writer.println();

    // 定义访客模式接口, 强制要求子类必须实现他(abstract)
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");
    writer.println();

    // 生成访客接口
    defineVisitor(writer, baseName, types);
    writer.println();

    // 生成每一个子类
    for(String type : types){
      String className = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim();
      defineType(writer, baseName, className, fields);
      writer.println();
    }

    writer.println("}");
    writer.close();
  }

  // 使用**访客模式**.
  private static void defineVisitor(
      PrintWriter writer, String baseName, List<String> types){
    // 定义泛型类接口 
    writer.println("  interface Visitor<R> {");

    // 为所有的类生成对应的访客函数.
    for(String type : types){
      String typeName = type.split(":")[0].trim();
      // R visitBinaryExpr(Binary expr);
      writer.println("    R visit" + typeName + baseName + "(" + 
                      typeName + " " + baseName.toLowerCase() + ");");
    }
    writer.println("  }");
  }

  // 生成Expr的每一个继承的类
  private static void defineType( PrintWriter writer, String baseName, String className, String fieldList) {
    // 生成类title
    writer.println("  static class " + className + " extends " + baseName + " {");
    // 构造函数
    writer.println("    " + className + "(" + fieldList + ") {");

    // 初始化语句
    String[] fields = fieldList.split(", ");
    for(String field : fields) {
      String name = field.split(" ")[1];
      writer.println("      this." + name + " = " + name + ";");
    }

    writer.println("    }");

    // 生成实现函数 [访客模式]
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" + className + baseName + "(this);");
    writer.println("    }");

    // 生成子类成员
    writer.println();
    for(String field : fields) {
      writer.println("    final " + field + ";");
    }
    writer.println("  }");
  }
}
