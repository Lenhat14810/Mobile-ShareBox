package com.example.sharebox.ui;

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.example.sharebox.R;
import com.example.sharebox.databinding.ComponentSystemErrorBinding;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import static com.example.sharebox.data.Models.*;

public final class CatalogPages {
    private CatalogPages(){}
    private static final String[] CATEGORY={"Tất cả","Điện tử","Laptop","Đồ gia dụng","Thể thao","Dụng cụ","Điện thoại","Sách","Nội thất","Khác"};
    private static final String[] ORDER={"Mới nhất","Giá tăng dần","Giá giảm dần"};
    private static final Set<String> DISTRICTS=new HashSet<>(Arrays.asList("thu duc","binh thanh","go vap","tan binh","tan phu","binh tan","phu nhuan",
            "binh chanh","hoc mon","nha be","cu chi","can gio"));
    static List<Product> available(State state,String query){
        String needle=search(query);
        return state.products.stream().filter(p->p.status==Status.ACTIVE &&
            search(p.name+" "+p.category+" "+p.location+" "+p.description).contains(needle)).collect(Collectors.toList());
    }

    public static final class Home extends Page {
        public Home(){super(R.layout.screen_home);}
        @Override protected void render(){
            frame.header.setVisibility(View.VISIBLE);
            View avatar=avatar(initials(state.profile.name));
            if(state.profile.photo!=null){
                ImageView pic=image(Art.CAMERA,state.profile.photo,false);
                pic.setPadding(0,0,0,0);pic.setScaleType(ImageView.ScaleType.CENTER_CROP);
                pic.setBackground(shape(color(R.color.sb_tint),40));pic.setClipToOutline(true);
                pic.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));avatar=pic;
            }
            click(avatar,R.drawable.sb_tint,()->host.openTab(R.id.profile));frame.header.addView(avatar);
            LinearLayout info=col();info.setPadding(dp(8),0,dp(4),0);
            String[] parts=state.profile.name.trim().split("\\s+");
            String name=parts.length>1?parts[parts.length-2]+" "+parts[parts.length-1]:parts[0];
            add(info,text(name,17,R.color.sb_ink,true),0);
            TextView finance=text("▣  Tài chính  ›",12,R.color.sb_accent,true);
            finance.setPadding(dp(9),dp(6),dp(9),dp(6));finance.setMinHeight(dp(34));
            click(finance,R.drawable.sb_tint,()->open(R.id.finance));add(info,finance,3);weight(frame.header,info);
            TextView ai=button("✧",false,()->open(R.id.ai_chat));ai.setContentDescription("ShareBox AI");frame.header.addView(ai);
            frame.header.addView(icon(R.drawable.sb_ic_moon,state.dark?"Chế độ sáng":"Chế độ tối",()->host.dark(!state.dark)));
            FrameLayout notification=new FrameLayout(requireContext());
            notification.addView(icon(R.drawable.sb_ic_bell,"Thông báo",()->open(R.id.notifications)),new FrameLayout.LayoutParams(dp(48),dp(48)));
            long unread=state.notices.stream().filter(n->!n.read).count();
            if(unread>0){
                TextView badge=tag(""+unread,R.color.sb_paper);badge.setTextColor(WHITE);badge.setBackground(shape(PINK,20));
                badge.setPadding(dp(5),dp(2),dp(5),dp(2));
                notification.addView(badge,new FrameLayout.LayoutParams(-2,-2,Gravity.END|Gravity.TOP));
            }
            frame.header.addView(notification,new LinearLayout.LayoutParams(dp(50),dp(48)));
            LinearLayout suggestions=col(), results=col();
            final EditText[] searchInput={null};
            final Consumer<String> submit=query->{
                if(searchInput[0]!=null&&!value(searchInput[0]).equals(query)){
                    searchInput[0].setText(query);
                    searchInput[0].setSelection(query.length());
                }
                memory.putString("query",query);memory.putString("committed",query);
                suggestions.removeAllViews();showResults(results,query);
                keyboardHide(frame.content);
            };
            LinearLayout bar=searchBar(frame.content,"Tìm laptop, máy ảnh, dụng cụ…",memory.getString("query",""),query->{
                memory.putString("query",query);
                if(query.isEmpty()){memory.remove("committed");results.removeAllViews();}
                suggest(suggestions,query,submit);
            },submit);
            searchInput[0]=(EditText)bar.getChildAt(1);
            bar.getChildAt(1).setOnFocusChangeListener((v,focus)->{if(focus)suggest(suggestions,memory.getString("query",""),submit);});
            frame.content.setFocusableInTouchMode(true);frame.content.requestFocus();
            add(frame.content,suggestions,0);add(frame.content,results,0);
            showResults(results,memory.getString("committed",""));
            hero();
            List<Product> products=available(state,"");
            featured("Sản phẩm nổi bật",products.stream().filter(p->p.id<=4).collect(Collectors.toList()));
            featured("Gợi ý dành cho bạn",products.stream().filter(p->p.id==7||p.id==3).collect(Collectors.toList()));
            featured("Đồ có thể thuê & mượn",products);
            featured("Sản phẩm mới đăng",products.stream().filter(Product::recent).collect(Collectors.toList()));
        }
        private void hero(){
            FrameLayout box=new FrameLayout(requireContext());box.setBackgroundResource(R.drawable.sb_gradient);box.setClipToOutline(true);
            ImageView art=new ImageView(requireContext());art.setImageResource(R.drawable.hero_camera);art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            FrameLayout.LayoutParams pic=new FrameLayout.LayoutParams(dp(140),dp(158),Gravity.END|Gravity.CENTER_VERTICAL);
            pic.rightMargin=dp(6);box.addView(art,pic);
            LinearLayout words=col();words.setPadding(dp(18),dp(16),0,dp(14));
            TextView kicker=meta("SHAREBOX");kicker.setTextColor(0xffdce6ff);add(words,kicker,0);
            TextView head=title("Đồ tốt, giá tốt –\nChia sẻ thông minh");head.setTextColor(WHITE);add(words,head,5);
            add(words,button("Khám phá ngay",false,()->host.openTab(R.id.explore)),10);
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(Math.min(230,getResources().getConfiguration().screenWidthDp-145)),-2,Gravity.START|Gravity.CENTER_VERTICAL);
            box.addView(words,lp);
            LinearLayout.LayoutParams card=new LinearLayout.LayoutParams(-1,dp(172));card.topMargin=dp(14);frame.content.addView(box,card);
        }
        private void featured(String title,List<Product> data){
            if(data.isEmpty())return;
            LinearLayout bar=row();weight(bar,this.title(title));bar.addView(button("Xem tất cả",false,()->host.openTab(R.id.explore)));
            add(frame.content,bar,12);
            embedded(frame.content,data.subList(0,Math.min(4,data.size())),2,p->productCard(p,true,false,()->{}));
        }
        private void suggest(LinearLayout group,String query,Consumer<String> search){
            group.removeAllViews();
            LinearLayout card=panel();
            add(card,title(query.trim().isEmpty()?"Tìm kiếm phổ biến":"Gợi ý sản phẩm"),0);
            List<String> terms=query.trim().isEmpty()?Arrays.asList("Máy ảnh","Laptop","Loa","Máy khoan","Xe đạp","Giáo trình"):
                    available(state,query).stream().map(p->p.name).limit(5).collect(Collectors.toList());
            if(terms.isEmpty())add(card,meta("Chưa có gợi ý khớp"),8);
            for(String term:terms){
                TextView result=button(term,false,()->search.accept(term));
                int start=search(term).indexOf(search(query));
                if(!query.isEmpty()&&start>=0&&start+query.length()<=term.length()){
                    SpannableString bold=new SpannableString(term);
                    bold.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),start,start+query.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    result.setText(bold);
                }
                add(card,result,3);
            }
            if(!query.trim().isEmpty())add(card,button("Tìm “"+query+"”",true,()->search.accept(query)),6);
            add(group,card,6);
        }
        private void showResults(LinearLayout group,String query){
            group.removeAllViews();
            if(query.trim().isEmpty())return;
            List<Product> results=available(state,query);
            add(group,title(results.size()+" sản phẩm cho “"+query+"”"),12);
            if(results.isEmpty())empty(group,"⌕","Không tìm thấy sản phẩm","Thử từ khóa khác hoặc xem tất cả sản phẩm.","Xem tất cả",()->host.openTab(R.id.explore));
            else embedded(group,results,2,p->productCard(p,true,false,()->{}));
        }
    }

    public static final class Explore extends Page {
        private LinearLayout filters, resultsBar;
        public Explore(){super(R.layout.screen_explore);}
        @Override protected void render(){
            frame.above.setPadding(dp(16),dp(12),dp(16),dp(3));
            searchBar(frame.above,"Tìm theo tên hoặc từ khóa",memory.getString("query",""),
                    s->{memory.putString("query",s);results();},null);
            filters=col();resultsBar=col();
            add(frame.above,filters,1);add(frame.above,resultsBar,4);
            filterControls();results();
        }
        private void filterControls(){
            filters.removeAllViews();
            chips(filters,CATEGORY,memory.getInt("category"),i->{memory.putInt("category",i);update();});
            HorizontalScrollView scroll=new HorizontalScrollView(requireContext());
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout row=row();
            String[] labels={"Miễn phí","Giá","TP.HCM","Tình trạng"};
            String[] keys={"free","price","city","condition"};
            for(int i=0;i<labels.length;i++){
                String key=keys[i];
                TextView t=button(labels[i],memory.getBoolean(key),()->{memory.putBoolean(key,!memory.getBoolean(key));update();});
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=dp(7);row.addView(t,lp);
            }
            scroll.addView(row);add(filters,scroll,8);
        }
        private void update(){filterControls();results();}
        private boolean hcm(Product p){
            String area=search(p.location);
            return DISTRICTS.contains(area)||area.matches("quan (?:[1-9]|1[0-2])")||
                    area.contains("tp.hcm")||area.contains("tphcm")||area.contains("ho chi minh")||area.contains("sai gon");
        }
        private void results(){
            if(resultsBar==null||frame==null)return;
            int c=memory.getInt("category"),order=memory.getInt("sort");
            boolean grid=!memory.getBoolean("list");
            List<Product> data=available(state,memory.getString("query","")).stream().filter(p->
                    (c==0||p.category.equals(CATEGORY[c]))&&(!memory.getBoolean("free")||p.rate==0)&&
                    (!memory.getBoolean("price")||(p.rate>0&&p.rate<=100000))&&
                    (!memory.getBoolean("city")||hcm(p))&&(!memory.getBoolean("condition")||!p.condition.equals("Đã dùng"))).collect(Collectors.toList());
            if(order==1)data.sort(Comparator.comparingLong(p->p.rate));
            else if(order==2)data.sort(Comparator.comparingLong((Product p)->p.rate).reversed());
            else data.sort(Comparator.comparing((Product p)->p.posted).reversed());
            resultsBar.removeAllViews();
            LinearLayout bar=row();
            TextView count=meta(data.size()+" kết quả");count.setMaxLines(1);weight(bar,count);
            bar.addView(icon(R.drawable.sb_ic_heart,"Yêu thích",()->open(R.id.wishlist)));
            Runnable sort=()->{memory.putInt("sort",(order+1)%ORDER.length);results();};
            if(getResources().getConfiguration().screenWidthDp<370)bar.addView(icon(R.drawable.sb_ic_list,"Sắp xếp · "+ORDER[order],sort));
            else bar.addView(button(ORDER[order],false,sort));
            ImageButton tiles=icon(R.drawable.sb_ic_grid,"Dạng lưới",()->{memory.putBoolean("list",false);results();});
            ImageButton rows=icon(R.drawable.sb_ic_list,"Dạng danh sách",()->{memory.putBoolean("list",true);results();});
            (grid?tiles:rows).setImageTintList(android.content.res.ColorStateList.valueOf(color(R.color.sb_accent)));
            bar.addView(tiles);bar.addView(rows);add(resultsBar,bar,4);
            frame.content.removeAllViews();
            if(data.isEmpty()){
                frame.items.setVisibility(View.GONE);frame.scroll.setVisibility(View.VISIBLE);
                empty(frame.content,"⌕","Không có kết quả","Thử từ khóa khác hoặc bỏ bớt bộ lọc.","Xóa bộ lọc",()->{memory.clear();refresh();});
            }else pageList(data,grid?2:1,p->productCard(p,grid,false,()->{}));
        }
    }

    public static final class Detail extends Page{
        public Detail(){super(R.layout.screen_product);}
        @Override protected void render(){
            Product p=product();
            if(p==null||p.status==Status.REMOVED){missing("Không tìm thấy sản phẩm");return;}
            gallery(p);
            LinearLayout box=frame.content;
            LinearLayout badge=row();
            if(p.hot()){
                ImageView hot=new ImageView(requireContext());hot.setImageResource(R.drawable.hot_art);hot.setContentDescription("HOT");
                badge.addView(hot,new LinearLayout.LayoutParams(dp(50),dp(45)));
            }
            if(p.recent())badge.addView(tag("NEW",R.color.sb_accent));
            badge.addView(tag(p.condition,R.color.sb_muted));add(box,badge,0);
            add(box,text(p.id==1?p.name+" kèm lens 18-55mm":p.name,23,R.color.sb_ink,true),10);
            add(box,text(p.price(),25,p.rate==0?R.color.sb_success:R.color.sb_accent,true),8);
            add(box,meta("⌖  "+p.location+"  ·  "+p.category),9);
            add(box,meta("★  4,9 · "+p.goodReviews+" đánh giá từ 4 sao · "+p.rentals+" lượt thuê"),7);
            add(box,meta("◇  Số lượng: "+p.quantity),7);
            LinearLayout deposit=panel();
            add(deposit,title(p.deposit==0?"Không cần đặt cọc":"Tiền đặt cọc "+money(p.deposit)),0);
            add(deposit,meta("Tách riêng khỏi phí thuê, hoàn lại khi trả đồ nguyên vẹn"),5);
            add(box,deposit,14);
            add(box,title("Mô tả"),15);add(box,meta(p.description.isEmpty()?"Chưa cập nhật":p.description),6);
            calendar(p);
            LinearLayout owner=panel();
            add(owner,productSummary(p,p.owner+" · 4,9 ★"),0);
            add(owner,meta("Thường phản hồi trong 15 phút · “Chủ đồ thân thiện, đồ đúng mô tả.”"),8);
            if(state.preferences.getOrDefault("Hiển thị số điện thoại",false)&&p.own&&state.transactions.stream().anyMatch(t->t.productId==p.id))
                add(owner,meta(state.profile.phone),6);
            add(box,owner,14);
            note(box,"Bạn có thể thương lượng qua chat trước khi gửi yêu cầu chính thức.",R.color.sb_accent);
            footer("Chat với chủ sản phẩm",true,()->openChat(p));
            footer("Yêu cầu giao dịch",false,()->{
                LocalDate start=selected("from");
                LocalDate end=selected("until");
                openRequest(p,start==null?DEMO:start,start!=null&&end!=null?(int)ChronoUnit.DAYS.between(start,end)+1:3);
            });
        }
        private void gallery(Product p){
            FrameLayout frameImage=new FrameLayout(requireContext());frameImage.setBackgroundColor(artBg(p.art));
            int count=p.images.isEmpty()?(p.id==1?4:1):p.images.size();
            int shown=memory.getInt("picture")%count;
            ImageView art=image(p.art,p.images.isEmpty()?null:p.images.get(shown),true);
            if(p.id==1&&p.images.isEmpty()){
                int[] sample={R.drawable.camera_art,R.drawable.camera_chat,R.drawable.camera_welcome,R.drawable.hero_camera};
                art.setImageResource(sample[shown]);
            }
            FrameLayout.LayoutParams picture=new FrameLayout.LayoutParams(-1,-1);picture.setMargins(dp(40),dp(28),dp(40),dp(20));frameImage.addView(art,picture);
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.START);lp.setMargins(dp(12),dp(10),dp(12),0);
            frameImage.addView(icon(R.drawable.sb_ic_back,"Quay lại",this::back),lp);
            LinearLayout menu=row();TextView heart=button("♡",false,()->{});favorite(heart,p);
            click(heart,R.drawable.sb_card,()->{vm.favorite(p.id);favorite(heart,p);});menu.addView(heart);
            menu.addView(icon(R.drawable.sb_ic_flag,"Báo cáo sản phẩm",()->report(p)));
            FrameLayout.LayoutParams right=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);right.setMargins(0,dp(10),dp(12),0);
            frameImage.addView(menu,right);
            if(count>1){
                FrameLayout.LayoutParams left=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.START|Gravity.CENTER_VERTICAL);
                FrameLayout.LayoutParams next=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.END|Gravity.CENTER_VERTICAL);
                frameImage.addView(button("‹",false,()->changeImage(shown-1,count)),left);
                frameImage.addView(button("›",false,()->changeImage(shown+1,count)),next);
                TextView dots=meta("");StringBuilder s=new StringBuilder();
                for(int i=0;i<count;i++)s.append(i==shown?"━ ":"• ");
                dots.setText(s.toString());dots.setTextColor(color(R.color.sb_accent));
                FrameLayout.LayoutParams dotsLp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
                dotsLp.bottomMargin=dp(12);frameImage.addView(dots,dotsLp);
            }
            frame.above.addView(frameImage,new LinearLayout.LayoutParams(-1,dp(Math.min(298,getResources().getConfiguration().screenHeightDp/3))));
        }
        private void changeImage(int image,int count){memory.putInt("picture",Math.floorMod(image,count));refresh();}
        private LocalDate selected(String key){return memory.containsKey(key)?LocalDate.ofEpochDay(memory.getLong(key)):null;}
        private void calendar(Product p){
            YearMonth month=YearMonth.from(DEMO).plusMonths(memory.getInt("month"));
            LocalDate start=selected("from"),end=selected("until");
            LinearLayout card=panel();LinearLayout bar=row();weight(bar,title("Lịch trống"));
            int offset=memory.getInt("month");
            bar.addView(button("‹",false,()->{memory.putInt("month",offset-1);refresh();}));
            bar.addView(meta("Tháng "+month.getMonthValue()+"/"+month.getYear()));
            bar.addView(button("›",false,()->{memory.putInt("month",offset+1);refresh();}));
            add(card,bar,0);
            LinearLayout weekdays=row();
            for(String l:new String[]{"T2","T3","T4","T5","T6","T7","CN"}){
                TextView day=meta(l);day.setGravity(Gravity.CENTER);weight(weekdays,day);
            }
            add(card,weekdays,6);
            int shift=month.atDay(1).getDayOfWeek().getValue()-1;
            for(int r=0;r<(shift+month.lengthOfMonth()+6)/7;r++){
                LinearLayout week=row();
                for(int c=0;c<7;c++){
                    int number=r*7+c-shift+1;
                    if(number<1||number>month.lengthOfMonth()){weight(week,new View(requireContext()));continue;}
                    LocalDate day=month.atDay(number);
                    boolean busy=!state.available(p.id,day,1);
                    boolean boundary=day.equals(start)||day.equals(end);
                    boolean between=start!=null&&end!=null&&day.isAfter(start)&&day.isBefore(end);
                    TextView cell=text(""+number,12,busy?R.color.sb_warning:R.color.sb_ink,boundary);
                    cell.setGravity(Gravity.CENTER);cell.setMinHeight(dp(47));
                    if(boundary){cell.setTextColor(WHITE);cell.setBackground(shape(0xff087b58,12));}
                    else if(between)cell.setBackground(shape(0xffd0efde,12));
                    else if(busy){cell.setBackgroundResource(R.drawable.sb_tint);cell.setEnabled(false);}
                    if(!busy)cell.setOnClickListener(v->selectDate(p,day,start,end));
                    weight(week,cell);
                }
                add(card,week,2);
            }
            add(card,meta("●  Đã chọn     ◌  Đã có người thuê"),8);
            if(start!=null&&end!=null){
                long days=ChronoUnit.DAYS.between(start,end)+1;
                add(card,text((p.rate==0?"Mượn ":"Thuê ")+days+" ngày · Phí "+money(days*p.rate)+" + cọc "+money(p.deposit),14,R.color.sb_success,true),8);
            }else add(card,meta("Chọn ngày nhận và ngày trả để xem chi phí."),8);
            add(frame.content,card,12);
        }
        private void selectDate(Product p,LocalDate day,LocalDate start,LocalDate end){
            long length=start==null?0:ChronoUnit.DAYS.between(start,day)+1;
            if(start==null||end!=null||length<1||length>14||!state.available(p.id,start,(int)length)){
                memory.putLong("from",day.toEpochDay());memory.remove("until");
            }else memory.putLong("until",day.toEpochDay());
            refresh();
        }
        private void report(Product p){
            String[] names={"Thông tin không đúng sự thật","Nghi ngờ lừa đảo","Hàng cấm hoặc vi phạm","Lý do khác"};
            int[] selected={-1};
            AlertDialog dialog=new AlertDialog.Builder(requireContext()).setTitle("Báo cáo sản phẩm")
                    .setSingleChoiceItems(names,-1,(d,i)->{selected[0]=i;((AlertDialog)d).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);})
                    .setNegativeButton("Đóng",null).setPositiveButton("Gửi báo cáo",(d,n)->{
                        state.reports.add(p.id+" · "+names[selected[0]]);
                        vm.changed();shortNote("Đã gửi báo cáo. Đội ngũ ShareBox sẽ xem xét trong 24 giờ.");
                    }).create();
            dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    public static final class Wishlist extends Page {
        public Wishlist(){super(R.layout.screen_wishlist);}
        @Override protected void render(){
            header();
            List<Product> items=state.favorites.stream().map(state::product).filter(p->p!=null&&p.status!=Status.REMOVED).collect(Collectors.toList());
            frame.header.addView(meta(items.size()+" sản phẩm"));
            if(items.isEmpty())empty(frame.content,"♡","Chưa có sản phẩm yêu thích",
                    "Nhấn vào biểu tượng trái tim trên sản phẩm để lưu lại và xem sau.","Khám phá sản phẩm",()->host.openTab(R.id.explore));
            else pageList(items,2,p->productCard(p,true,true,this::refresh));
        }
    }

    public static final class States extends Page{
        private final Handler handler=new Handler(Looper.getMainLooper());
        private final List<ObjectAnimator> animations=new ArrayList<>();
        public States(){super(R.layout.screen_states);}
        @Override protected void render(){
            stopAnimations();header();
            int mode=memory.getInt("state",2);
            frame.above.setPadding(dp(16),0,dp(16),dp(5));
            chips(frame.above,new String[]{"Đang tải","Không có kết quả","Lỗi kết nối"},mode,i->{handler.removeCallbacksAndMessages(null);select(i);});
            searchBar(frame.above,"Tìm theo tên hoặc từ khóa",memory.getString("query",""),s->memory.putString("query",s),s->select(1));
            if(mode==0){
                for(int i=0;i<2;i++){
                    LinearLayout tiles=row();
                    for(int j=0;j<2;j++){
                        View card=new View(requireContext());card.setBackgroundResource(R.drawable.sb_tint);
                        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(154),1);lp.setMargins(dp(4),0,dp(4),0);tiles.addView(card,lp);
                        ObjectAnimator alpha=ObjectAnimator.ofFloat(card,View.ALPHA,.42f,1f);alpha.setDuration(700);
                        alpha.setRepeatMode(ObjectAnimator.REVERSE);alpha.setRepeatCount(ObjectAnimator.INFINITE);alpha.start();animations.add(alpha);
                    }
                    add(frame.content,tiles,12);
                }
            }else if(mode==1){
                empty(frame.content,"⌕","Không tìm thấy sản phẩm","Thử đổi từ khóa, bỏ bớt bộ lọc hoặc hỏi ShareBox AI để được gợi ý.",
                        "Xóa bộ lọc",()->host.openTab(R.id.explore));
                add(frame.content,button("Hỏi ShareBox AI",false,()->open(R.id.ai_chat)),8);
            }else{
                space(frame.content,66);
                ComponentSystemErrorBinding error=ComponentSystemErrorBinding.inflate(getLayoutInflater(),frame.content,false);
                click(error.retry,R.drawable.sb_blue,()->{
                    select(0);handler.postDelayed(()->{if(frame!=null)host.openTab(R.id.explore);},900);
                });
                add(frame.content,error.getRoot(),4);
            }
        }
        private void select(int mode){memory.putInt("state",mode);refresh();}
        private void stopAnimations(){for(ObjectAnimator a:animations)a.cancel();animations.clear();}
        @Override public void onDestroyView(){handler.removeCallbacksAndMessages(null);stopAnimations();super.onDestroyView();}
    }
}
