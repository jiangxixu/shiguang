package io.github.jiangxixu.shiguang;
import java.util.*;
public class VisibleNodesTest {
    static final class Node {boolean visible,sensitive;List<Node> children=new ArrayList<>();Node(boolean v,boolean s,Node... nodes){visible=v;sensitive=s;children.addAll(Arrays.asList(nodes));}}
    static VisibleNodes.Tree<Node> tree=new VisibleNodes.Tree<Node>(){
        public boolean visible(Node n){return n.visible;}public boolean sensitive(Node n){return n.sensitive;}
        public int children(Node n){return n.children.size();}public Node child(Node n,int i){return n.children.get(i);}
    };
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        Node title=new Node(true,false),share=new Node(true,false),save=new Node(true,false),passwordChild=new Node(true,false);
        Node root=new Node(false,false,new Node(false,false,title,new Node(false,false,share)),save,new Node(false,true,passwordChild));
        List<Node> found=VisibleNodes.collect(root,tree);
        check(found.contains(title)&&found.contains(share)&&found.contains(save));check(!found.contains(root)&&!found.contains(passwordChild));
        check(VisibleNodes.collect(null,tree).isEmpty());
        Node icon=new Node(true,false),label=new Node(true,false),chip=new Node(true,false,new Node(false,false,new Node(false,false,icon,label)));
        List<Node> chipNodes=VisibleNodes.collect(chip,tree);
        check(VisibleNodes.uniqueTarget(chipNodes,n->n==icon||n==label?chip:null)==chip);
        check(VisibleNodes.uniqueTarget(chipNodes,n->n==icon||n==label?n:null)==null);
        check(VisibleNodes.uniqueTarget(chipNodes,n->null)==null);
        Node cycle=new Node(false,false);cycle.children.add(cycle);check(VisibleNodes.collect(cycle,tree).isEmpty());
        Node many=new Node(false,false);for(int i=0;i<5000;i++)many.children.add(new Node(true,false));check(VisibleNodes.collect(many,tree).size()==800);
        System.out.println("Visible title, share and save controls survive hidden wrappers; sensitive subtrees and traversal are bounded.");
    }
}
