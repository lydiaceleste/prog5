package Translate;
import Symbol.Symbol;
import Tree.BINOP;
import Tree.CJUMP;
import Temp.Temp;
import Temp.Label;

public class Translate {
  public Frame.Frame frame;
  public Translate(Frame.Frame f) {
    frame = f;
  }
  private Frag frags;

  //generating the intermediate code for a function
  public void procEntryExit(Level level, Exp body) {
    Frame.Frame myframe = level.frame;
    Tree.Exp bodyExp = body.unEx();
    Tree.Stm bodyStm;
    if (bodyExp != null)
      bodyStm = MOVE(TEMP(myframe.RV()), bodyExp);
    else
      bodyStm = body.unNx();
    ProcFrag frag = new ProcFrag(myframe.procEntryExit1(bodyStm), myframe);
    frag.next = frags;
    frags = frag;
  }
  //retrieving generated intermediate code fragments
  public Frag getResult() {
    return frags;
  }

  private static Tree.Exp CONST(int value) {
    return new Tree.CONST(value);
  }
  private static Tree.Exp NAME(Label label) {
    return new Tree.NAME(label);
  }
  private static Tree.Exp TEMP(Temp temp) {
    return new Tree.TEMP(temp);
  }
  private static Tree.Exp BINOP(int binop, Tree.Exp left, Tree.Exp right) {
    return new Tree.BINOP(binop, left, right);
  }
  private static Tree.Exp MEM(Tree.Exp exp) {
    return new Tree.MEM(exp);
  }
  private static Tree.Exp CALL(Tree.Exp func, Tree.ExpList args) {
    return new Tree.CALL(func, args);
  }
  private static Tree.Exp ESEQ(Tree.Stm stm, Tree.Exp exp) {
    if (stm == null)
      return exp;
    return new Tree.ESEQ(stm, exp);
  }

  private static Tree.Stm MOVE(Tree.Exp dst, Tree.Exp src) {
    return new Tree.MOVE(dst, src);
  }
  private static Tree.Stm UEXP(Tree.Exp exp) {
    return new Tree.UEXP(exp);
  }
  private static Tree.Stm JUMP(Label target) {
    return new Tree.JUMP(target);
  }
  private static Tree.Stm CJUMP(int relop, Tree.Exp l, Tree.Exp r, Label t, Label f) {
    return new Tree.CJUMP(relop, l, r, t, f);
  }
  private static Tree.Stm SEQ(Tree.Stm left, Tree.Stm right) {
    if (left == null)
      return right;
    if (right == null)
      return left;
    return new Tree.SEQ(left, right);
  }
  private static Tree.Stm LABEL(Label label) {
    return new Tree.LABEL(label);
  }

  private static Tree.ExpList ExpList(Tree.Exp head, Tree.ExpList tail) {
    return new Tree.ExpList(head, tail);
  }
  private static Tree.ExpList ExpList(Tree.Exp head) {
    return ExpList(head, null);
  }
  private static Tree.ExpList ExpList(ExpList exp) {
    if (exp == null)
      return null;
    return ExpList(exp.head.unEx(), ExpList(exp.tail));
  }

  public Exp Error() {
    return new Ex(CONST(0));
  }

  public Exp SimpleVar(Access access, Level level) {
    Tree.Exp framePointer = TEMP(frame.FP());
    Level currentLevel = level;
    
    while (currentLevel != access.home) {
      framePointer = currentLevel.parent.frame.formals.head.exp(framePointer);
      currentLevel = currentLevel.parent;
    }
  
    Tree.Exp location = access.acc.exp(framePointer);
    return new Ex(location);
  }


  public Exp FieldVar(Exp record, int index) {
    return Error();
  }

  public Exp SubscriptVar(Exp array, Exp index) {
    return Error();
  }

  public Exp NilExp() {
    return Error();
  }

  public Exp IntExp(int value) {
    return new Ex(CONST(value));
  }

  private java.util.Hashtable strings = new java.util.Hashtable();
  public Exp StringExp(String lit) {
    String u = lit.intern();
    Label lab = (Label)strings.get(u);
    if (lab == null) {
      lab = new Label();
      strings.put(u, lab);
      DataFrag frag = new DataFrag(frame.string(lab, u));
      frag.next = frags;
      frags = frag;
    }
    return new Ex(NAME(lab));
  }

  private Tree.Exp CallExp(Symbol f, ExpList args, Level from) {
    return frame.externalCall(f.toString(), ExpList(args));
  }
  private Tree.Exp CallExp(Level f, ExpList args, Level from) {
    throw new Error("Translate.CallExp unimplemented");
  }

  public Exp FunExp(Symbol f, ExpList args, Level from) {
    return new Ex(CallExp(f, args, from));
  }
  public Exp FunExp(Level f, ExpList args, Level from) {
    return new Ex(CallExp(f, args, from));
  }
  public Exp ProcExp(Symbol f, ExpList args, Level from) {
    return new Nx(UEXP(CallExp(f, args, from)));
  }
  public Exp ProcExp(Level f, ExpList args, Level from) {
    return new Nx(UEXP(CallExp(f, args, from)));
  }

  public Exp OpExp(int op, Exp left, Exp right) {
    return new Ex(BINOP(op, left.unEx(), right.unEx()));
  }

  public Exp StrOpExp(int op, Exp left, Exp right) {
    return new Ex(BINOP(op, left.unEx(), right.unEx()));
  }

  public Exp RecordExp(ExpList init) {
    return Error();
  }

  public Exp SeqExp(ExpList e) {
    return Error();
  }

  public Exp AssignExp(Exp lhs, Exp rhs) {
    return new Nx(MOVE(lhs.unEx(), rhs.unEx()));
  }

  public Exp IfExp(Exp cc, Exp aa, Exp bb) {
    return new IfThenElseExp(cc, aa, bb);
  }

  public Exp WhileExp(Exp test, Exp body, Label done) {
    Label testLabel = new Label();
    Label bodyLabel = new Label();
    Tree.Stm testStm = SEQ(LABEL(testLabel), test.unCx(bodyLabel, done));
    Tree.Stm bodyStm = SEQ(LABEL(bodyLabel), body.unNx());
    Tree.Stm left = SEQ(testStm, SEQ(bodyStm, JUMP(testLabel)));
    return new Nx(SEQ(left, LABEL(done)));
}

  public Exp ForExp(Access i, Exp lo, Exp hi, Exp body, Label done) {
      return Error();
  }

  public Exp ForExp(Exp id, Exp lo, Exp hi, Exp body, Label done) {
      return Error();

  }

  public Exp BreakExp(Label done) {
    return new Nx(JUMP(done));
  }

  public Exp LetExp(ExpList lets, Exp body) {
      if (lets == null) {
        return body;
    } else {
        Tree.Stm seq = null;
        for (ExpList e = lets; e != null; e = e.tail) {
            seq = (seq == null) ? e.head.unNx() : SEQ(seq, e.head.unNx());
        }
        return new Nx(SEQ(seq, body.unNx()));
    }
  }

  public Exp ArrayExp(Exp size, Exp init) {
      return Error();
  }

  public Exp VarDec(Access a, Exp init) {
    return new Nx(MOVE(a.acc.exp(TEMP(frame.FP())), init.unEx()));
  }

  public Exp TypeDec() {
    return Error();
  }

  public Exp FunctionDec() {
    return new Nx(null);
  }
}
