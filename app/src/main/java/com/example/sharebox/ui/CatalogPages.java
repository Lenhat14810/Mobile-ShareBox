package com.example.sharebox.ui;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.example.sharebox.R;
import com.example.sharebox.data.ListingFilters.Condition;
import com.example.sharebox.data.ListingFilters.Location;
import com.example.sharebox.data.ListingFilters.PriceRange;
import com.example.sharebox.databinding.ComponentSystemErrorBinding;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import static com.example.sharebox.data.Models.*;

public final class CatalogPages {
    private CatalogPages(){}
    private static final String[] CATEGORY={"Tất cả","Điện tử","Laptop","Đồ gia dụng","Thể thao","Dụng cụ","Điện thoại","Sách","Nội thất","Khác"};
    private static final String[] ORDER={"Mới nhất","Giá tăng dần","Giá giảm dần"};
    static List<Product> available(State state,String query){
        String needle=search(query);
        return state.products.stream().filter(p->p.status==Status.ACTIVE &&
            search(p.name+" "+p.category+" "+p.location+" "+p.description).contains(needle)).collect(Collectors.toList());
    }

    public static final class Home extends Page {
        public Home(){super(R.layout.screen_home);}
        @Override protected void render(){
            frame.header.setVisibility(View.VISIBLE);
            ConstraintLayout identity=new ConstraintLayout(requireContext());
            frame.header.addView(identity,new LinearLayout.LayoutParams(-1,-2));
            View avatar=avatar(initials(state.profile.name));
            if(state.profile.photo!=null){
                ImageView photo=image(Art.CAMERA,state.profile.photo,false);
                photo.setPadding(0,0,0,0);
                photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                avatar=photo;
            }else ((TextView)avatar).setTextSize(20);
            avatar.setId(View.generateViewId());
            click(avatar,0,()->host.openTab(R.id.profile));
            GradientDrawable circle=new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(color(R.color.sb_tint));
            avatar.setBackground(new RippleDrawable(ColorStateList.valueOf(0x24265de6),circle,null));
            avatar.setClipToOutline(true);
            ConstraintLayout.LayoutParams avatarSize=new ConstraintLayout.LayoutParams(dp(64),dp(64));
            avatarSize.startToStart=ConstraintLayout.LayoutParams.PARENT_ID;
            avatarSize.topToTop=ConstraintLayout.LayoutParams.PARENT_ID;
            avatarSize.topMargin=dp(4);
            identity.addView(avatar,avatarSize);

            ImageButton theme=headerIcon(state.dark?R.drawable.sb_ic_sun:R.drawable.sb_ic_moon,
                    state.dark?"Chế độ sáng":"Chế độ tối",()->host.dark(!state.dark));
            theme.setId(View.generateViewId());
            FrameLayout notification=new FrameLayout(requireContext());
            notification.setId(View.generateViewId());
            notification.addView(headerIcon(R.drawable.sb_ic_bell,"Thông báo",()->open(R.id.notifications)),
                    new FrameLayout.LayoutParams(dp(48),dp(48)));
            long unread=state.notices.stream().filter(n->!n.read).count();
            if(unread>0){
                TextView badge=text(Long.toString(unread),12,R.color.sb_paper,true);
                badge.setBackground(shape(color(R.color.sb_error),20));
                badge.setPadding(dp(5),dp(2),dp(5),dp(2));
                notification.addView(badge,new FrameLayout.LayoutParams(-2,-2,Gravity.END|Gravity.TOP));
            }
            ConstraintLayout.LayoutParams notificationSize=alignToAvatar(dp(52),dp(48),avatar);
            notificationSize.endToEnd=ConstraintLayout.LayoutParams.PARENT_ID;
            identity.addView(notification,notificationSize);
            ConstraintLayout.LayoutParams themeSize=alignToAvatar(dp(48),dp(48),avatar);
            themeSize.endToStart=notification.getId();
            themeSize.setMarginEnd(dp(8));
            identity.addView(theme,themeSize);

            String[] parts=state.profile.name.trim().split("\\s+");
            String name=parts.length>1?parts[parts.length-2]+" "+parts[parts.length-1]:parts[0];
            TextView username=text(name,20,R.color.sb_ink,true);
            username.setId(View.generateViewId());
            username.setSingleLine(true);
            username.setEllipsize(TextUtils.TruncateAt.END);
            ConstraintLayout.LayoutParams nameSize=alignToAvatar(0,-2,avatar);
            nameSize.startToEnd=avatar.getId();
            nameSize.endToStart=theme.getId();
            nameSize.setMarginStart(dp(10));
            nameSize.setMarginEnd(dp(6));
            identity.addView(username,nameSize);

            TextView finance=textAction("▣  Tài chính  ›",()->open(R.id.finance));
            finance.setTextSize(12);
            finance.setSingleLine(true);
            finance.setEllipsize(TextUtils.TruncateAt.END);
            finance.setGravity(Gravity.TOP|Gravity.START);
            finance.setPaddingRelative(0,dp(3),0,0);
            ConstraintLayout.LayoutParams financeSize=new ConstraintLayout.LayoutParams(0,-2);
            financeSize.startToStart=username.getId();
            financeSize.endToEnd=username.getId();
            financeSize.topToBottom=username.getId();
            identity.addView(finance,financeSize);

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
            if(host.showsHomeAiShortcut()){
                add(frame.content,textAction(getString(R.string.sb_open_ai),()->open(R.id.ai_chat)),4);
            }
            List<Product> products=available(state,"");
            featured("Sản phẩm nổi bật",products.stream().filter(Product::hot).collect(Collectors.toList()));
            featured("Sản phẩm mới đăng",products.stream().filter(p->!p.hot()&&p.recent())
                    .sorted(Comparator.comparing((Product p)->p.posted).reversed()).collect(Collectors.toList()));
            featured("Khám phá thêm",products.stream().filter(p->!p.hot()&&!p.recent()).collect(Collectors.toList()));
        }
        private ConstraintLayout.LayoutParams alignToAvatar(int width,int height,View avatar){
            ConstraintLayout.LayoutParams size=new ConstraintLayout.LayoutParams(width,height);
            size.topToTop=avatar.getId();
            size.bottomToBottom=avatar.getId();
            return size;
        }
        private ImageButton headerIcon(int drawable,String label,Runnable action){
            ImageButton button=icon(drawable,label,false,action);
            button.setPadding(dp(9),dp(9),dp(9),dp(9));
            return button;
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
            LinearLayout bar=row();weight(bar,this.title(title));bar.addView(textAction("Xem tất cả",()->host.openTab(R.id.explore)));
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
        private static final PriceRange[] PRICES=PriceRange.values();
        private static final Location[] LOCATIONS=Location.values();
        private static final Condition[] CONDITIONS=Condition.values();
        private static final String[] PRICE_LABELS=Arrays.stream(PRICES).map(price->price.label).toArray(String[]::new);
        private static final String[] LOCATION_LABELS=Arrays.stream(LOCATIONS).map(location->location.label).toArray(String[]::new);
        private static final String[] CONDITION_LABELS=Arrays.stream(CONDITIONS).map(condition->condition.label).toArray(String[]::new);
        private LinearLayout filters,resultsBar;
        public Explore(){super(R.layout.screen_explore);}
        @Override protected void render(){
            frame.above.setPadding(dp(16),dp(12),dp(16),dp(3));
            searchBar(frame.above,"Tìm theo tên hoặc từ khóa",memory.getString("query",""),
                    s->{memory.putString("query",s);results();},null);
            filters=col();
            add(frame.above,filters,8);
            renderFilters();
            resultsBar=col();
            add(frame.above,resultsBar,4);
            results();
        }
        private void renderFilters(){
            filters.removeAllViews();
            HorizontalScrollView categories=filterScroll();
            LinearLayout types=filterGroup(CATEGORY,i->memory.getInt("category")==i,
                    i->memory.putInt("category",i));
            categories.addView(types);
            add(filters,categories,0);
            int category=memory.getInt("category");
            if(category>2)categories.post(()->categories.scrollTo(types.getChildAt(category).getLeft(),0));

            TextView free=filterOption("Miễn phí");
            free.setSelected(memory.getBoolean("free"));
            click(free,R.drawable.sb_explore_sort,()->{
                boolean active=!memory.getBoolean("free");
                memory.putBoolean("free",active);
                free.setSelected(active);
                results();
            });
            TextView price=picker(getString(R.string.sb_daily_price_filter),getString(R.string.sb_daily_price_title),
                    "price_range",PRICE_LABELS);
            TextView location=picker("Địa điểm","Địa điểm","location_filter",LOCATION_LABELS);
            TextView condition=picker("Tình trạng","Tình trạng","condition_filter",CONDITION_LABELS);
            var configuration=getResources().getConfiguration();
            boolean twoRows=configuration.screenWidthDp<320||configuration.fontScale>1.2f;
            if(twoRows){
                addFilterRow(free,price);
                addFilterRow(location,condition);
            }else addFilterRow(free,price,location,condition);
        }
        private void clearFilters(){
            for(String key:new String[]{"category","free","price_range","location_filter","condition_filter"}){
                memory.remove(key);
            }
            renderFilters();
            results();
        }
        private HorizontalScrollView filterScroll(){
            HorizontalScrollView scroll=new HorizontalScrollView(requireContext());
            scroll.setHorizontalScrollBarEnabled(false);
            return scroll;
        }
        private void addFilterRow(TextView... controls){
            LinearLayout group=row();
            group.setGravity(Gravity.TOP);
            for(int i=0;i<controls.length;i++){
                TextView control=controls[i];
                control.setTextSize(12);
                control.setMaxLines(2);
                control.setEllipsize(TextUtils.TruncateAt.END);
                control.setPaddingRelative(dp(3),dp(6),dp(3),dp(6));
                LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,-2,1);
                if(i>0)size.setMarginStart(dp(6));
                group.addView(control,size);
            }
            add(filters,group,6);
        }
        private TextView picker(String label,String title,String key,String[] labels){
            TextView control=filterOption(label+" ▾");
            showPickerSelection(control,title,labels,memory.getInt(key,-1));
            click(control,R.drawable.sb_explore_sort,()->{
                int previous=memory.getInt(key,-1);
                IntConsumer apply=index->{
                    memory.putInt(key,index);
                    showPickerSelection(control,title,labels,index);
                    results();
                };
                AlertDialog.Builder dialog=new AlertDialog.Builder(requireContext())
                        .setTitle(title)
                        .setSingleChoiceItems(labels,previous,(window,index)->{
                            apply.accept(index);
                            window.dismiss();
                        })
                        .setNegativeButton("Đóng",null);
                if(previous>=0)dialog.setNeutralButton("Bỏ lọc",(window,which)->apply.accept(-1));
                dialog.show();
            });
            return control;
        }
        private void showPickerSelection(TextView option,String title,String[] labels,int index){
            option.setSelected(index>=0);
            option.setContentDescription(index<0?title:title+" · "+labels[index]);
        }
        private TextView filterOption(String label){
            TextView option=text(label,13,R.color.sb_muted,true);
            option.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_selected},{}},
                    new int[]{color(R.color.sb_accent),color(R.color.sb_muted)}));
            option.setGravity(Gravity.CENTER);
            option.setMinHeight(dp(48));
            option.setMinWidth(dp(48));
            return option;
        }
        private LinearLayout filterGroup(String[] labels,IntPredicate selected,IntConsumer change){
            LinearLayout group=row();
            group.setBackgroundResource(R.drawable.sb_card);
            group.setPaddingRelative(dp(3),dp(2),dp(3),dp(2));
            group.setClipToOutline(true);
            for(int i=0;i<labels.length;i++){
                int index=i;
                TextView option=filterOption(labels[i]);
                option.setSelected(selected.test(i));
                click(option,R.drawable.sb_explore_filter_fill,()->{
                    change.accept(index);
                    for(int j=0;j<group.getChildCount();j++)group.getChildAt(j).setSelected(selected.test(j));
                    results();
                });
                option.setPaddingRelative(dp(8),0,dp(8),0);
                group.addView(option,new LinearLayout.LayoutParams(-2,-2));
            }
            return group;
        }
        private void results(){
            if(resultsBar==null||frame==null)return;
            int c=memory.getInt("category"),order=memory.getInt("sort");
            boolean grid=!memory.getBoolean("list"),free=memory.getBoolean("free");
            int price=memory.getInt("price_range",-1),location=memory.getInt("location_filter",-1);
            int condition=memory.getInt("condition_filter",-1);
            List<Product> data=available(state,memory.getString("query","")).stream().filter(p->
                    (c==0||p.category.equals(CATEGORY[c]))&&(!free||p.rate==0)&&
                    (price<0||PRICES[price].contains(p.rate))&&
                    (location<0||Location.fromAddress(p.location)==LOCATIONS[location])&&
                    (condition<0||Condition.fromStored(p.condition)==CONDITIONS[condition])).collect(Collectors.toList());
            if(order==1)data.sort(Comparator.comparingLong(p->p.rate));
            else if(order==2)data.sort(Comparator.comparingLong((Product p)->p.rate).reversed());
            else data.sort(Comparator.comparing((Product p)->p.posted).reversed());
            resultsBar.removeAllViews();
            LinearLayout bar=row();
            var configuration=getResources().getConfiguration();
            boolean compact=configuration.screenWidthDp<400||configuration.fontScale>1.15f;
            boolean filtered=c!=0||free||price>=0||location>=0||condition>=0;
            if(filtered){
                List<String> selections=new ArrayList<>();
                if(price>=0)selections.add(PRICE_LABELS[price]);
                if(location>=0)selections.add(LOCATION_LABELS[location]);
                if(condition>=0)selections.add(CONDITION_LABELS[condition]);
                if(!selections.isEmpty())add(resultsBar,meta(String.join(" · ",selections)),4);
                LinearLayout filterStatus=row();
                weight(filterStatus,meta(data.size()+" kết quả"));
                filterStatus.addView(textAction(getString(R.string.sb_clear_filters),this::clearFilters));
                add(resultsBar,filterStatus,0);
            }
            if(!compact)weight(bar,new Space(requireContext()));

            bar.addView(icon(R.drawable.sb_ic_heart,"Yêu thích",false,()->open(R.id.wishlist)));
            TextView sort=text(ORDER[order],14,order==0?R.color.sb_accent:R.color.sb_ink,true);
            sort.setGravity(Gravity.CENTER);
            sort.setMinHeight(dp(48));
            sort.setMaxLines(2);
            sort.setEllipsize(TextUtils.TruncateAt.END);
            sort.setSelected(order==0);
            click(sort,R.drawable.sb_explore_sort,()->{memory.putInt("sort",(order+1)%ORDER.length);results();});
            sort.setPaddingRelative(dp(8),0,dp(8),0);
            sort.setContentDescription("Sắp xếp · "+ORDER[order]);
            LinearLayout.LayoutParams sortSize=new LinearLayout.LayoutParams(compact?0:-2,-2,compact?1:0);
            sortSize.setMarginStart(dp(8));
            sortSize.setMarginEnd(dp(8));
            bar.addView(sort,sortSize);

            bar.addView(viewSwitcher(grid));
            add(resultsBar,bar,4);
            frame.content.removeAllViews();
            if(data.isEmpty()){
                frame.items.setVisibility(View.GONE);frame.scroll.setVisibility(View.VISIBLE);
                empty(frame.content,"⌕","Không có kết quả",
                        filtered?"Thử từ khóa khác hoặc bỏ bớt bộ lọc.":"Thử một từ khóa khác.","",null);
            }else pageList(data,grid?2:1,p->productCard(p,grid,false,()->{}));
        }
        private View viewSwitcher(boolean grid){
            FrameLayout surface=new FrameLayout(requireContext());
            surface.setBackgroundResource(R.drawable.sb_card);
            surface.setElevation(dp(2));
            surface.setPadding(dp(4),dp(3),dp(4),dp(3));

            LinearLayout track=row();
            track.setBackground(shape(color(R.color.sb_canvas),12));
            track.addView(viewMode(R.drawable.sb_ic_grid,"Dạng lưới",grid,false));
            track.addView(viewMode(R.drawable.sb_ic_list,"Dạng danh sách",!grid,true));
            surface.addView(track,new FrameLayout.LayoutParams(dp(96),dp(48)));
            return surface;
        }
        private ImageButton viewMode(int drawable,String label,boolean selected,boolean list){
            ImageButton option=icon(drawable,label,false,()->{memory.putBoolean("list",list);results();});
            option.setSelected(selected);
            if(selected){
                option.setBackgroundResource(R.drawable.sb_explore_view_selected);
                option.setElevation(dp(1));
                option.setImageTintList(ColorStateList.valueOf(color(R.color.sb_accent)));
            }
            option.setScaleType(ImageView.ScaleType.FIT_CENTER);
            option.setPadding(dp(12),dp(12),dp(12),dp(12));
            return option;
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
            add(box,meta("Bạn có thể thương lượng qua chat trước khi gửi yêu cầu chính thức."),10);
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
            LinearLayout menu=row();
            ImageButton heart=icon(R.drawable.sb_ic_heart,"Yêu thích",()->{});
            bindFavorite(heart,p,null);
            menu.addView(heart);
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
