package io.github.jiangxixu.shiguang;

/** A capture is tied to this copy operation and one real source-platform URL. */
final class CapturedLink {
    final String url,title;
    private CapturedLink(String url,String title){this.url=url;this.title=title;}
    static CapturedLink parse(String pkg,String text,long copiedAt,long notBefore,String pageTitle){
        if(notBefore<=0||copiedAt<notBefore-100||text==null||text.length()>100000)return null;
        String selected=null;
        for(String link:ShareLinks.candidates(text))if(FavoriteRules.contentUrl(pkg,link)){
            if(selected!=null&&!selected.equals(link))return null;selected=link;
        }
        if(selected==null)return null;
        String title=ShareLinks.sharedTitle(text);
        if(FavoriteRules.supported(pkg)&&!PageTitles.clean(pageTitle).isEmpty())title=PageTitles.clean(pageTitle);
        if(title.isEmpty())try{title=new java.net.URI(selected).getHost();}catch(Exception ignored){}
        return new CapturedLink(selected,title==null?"":title);
    }
}
