package io.github.jiangxixu.shiguang;
import java.util.*;
/** Some UI frameworks expose visible controls below non-visible layout wrappers. */
final class VisibleNodes {
    static <T> T uniqueTarget(List<T> nodes,java.util.function.Function<T,T> action){
        T found=null;
        for(T node:nodes){T target=action.apply(node);if(target==null)continue;if(found!=null&&!found.equals(target))return null;found=target;}
        return found;
    }
    interface Tree<T>{boolean visible(T node);boolean sensitive(T node);int children(T node);T child(T node,int index);}
    static <T> List<T> collect(T root,Tree<T> tree){
        List<T> result=new ArrayList<>();if(root==null)return result;
        ArrayDeque<T> queue=new ArrayDeque<>();queue.add(root);int visited=0;
        while(!queue.isEmpty()&&visited++<2000&&result.size()<800){
            T n=queue.remove();if(tree.sensitive(n))continue;
            if(tree.visible(n))result.add(n);
            for(int i=0;i<tree.children(n)&&queue.size()<2000;i++){T child=tree.child(n,i);if(child!=null)queue.add(child);}
        }
        return result;
    }
}
