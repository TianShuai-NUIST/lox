package com.craftinginterpreters.lox;

// implements 承诺这个类一定会实现所有的虚函数
// AstPrinter 继承了Expr的接口Vistor.
class AstPrinter implements Expr.Visitor<String> {
  // 用于打印AST表达式的可读形势
  String print(Expr expr){
    return expr.accept(this);
  }

  // 实例化输出的类型
  @Override
  public String visitBinaryExpr(Expr.Binary expr){
    return parenthesize(expr.operator.lexeme,
                        expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  // 把AST表达式输出成一个可读的信息
  private String parenthesize(String name, Expr... exprs){
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for(Expr expr : exprs) {
      builder.append(" ");
      // 递归调用
      // accept -> Expr的子类 -> Expr 的visitro接口 -> 上面的实现 -> parenthesize
      builder.append(expr.accept(this));
    }
    builder.append(")");

    return builder.toString();
  }
}
