package com.example.sharebox.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.example.sharebox.R;
import com.example.sharebox.data.ListingFilters.Condition;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static com.example.sharebox.data.Models.*;

public final class ProfilePages {
    private ProfilePages(){}
    private static final String EMAIL_NOTE="Email dùng để đăng nhập nên không thể thay đổi.";
    private static final String[] CATEGORIES={"Điện tử","Điện thoại","Laptop","Đồ gia dụng","Thời trang","Thể thao","Sách","Dụng cụ","Nội thất","Khác"};
    private static final Condition[] CONDITIONS=Condition.values();
    private static final String[] CONDITION_LABELS=Arrays.stream(CONDITIONS).map(c->c.label).toArray(String[]::new);

    private abstract static class AccountPage extends Page {
        AccountPage(int layout){super(layout);}
        protected void menu(LinearLayout parent,String icon,String label,String detail,int unread,Runnable action){
            LinearLayout line=row();line.setPadding(dp(8),dp(4),dp(8),dp(4));line.setMinimumHeight(dp(53));
            TextView symbol=avatar(icon);symbol.setTextSize(16);symbol.setLayoutParams(new LinearLayout.LayoutParams(dp(36),dp(36)));
            line.addView(symbol);
            LinearLayout info=col();info.setPadding(dp(10),0,dp(7),0);
            add(info,text(label,14,label.equals("Đăng xuất")?R.color.sb_error:R.color.sb_ink,true),0);
            if(!detail.isEmpty())add(info,meta(detail),2);
            weight(line,info);
            if(unread>0){TextView dot=tag(""+unread,R.color.sb_paper);dot.setTextColor(WHITE);dot.setBackground(shape(PINK,20));line.addView(dot);}
            TextView arrow=meta("  ›");arrow.setTextSize(20);line.addView(arrow);
            click(line,0,action);
            add(parent,line,0);
            View rule=new View(requireContext());rule.setBackgroundColor(color(R.color.sb_stroke));
            parent.addView(rule,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        protected void about(){
            new AlertDialog.Builder(requireContext()).setTitle("Về ShareBox")
                    .setMessage("ShareBox 1.0.0\nChia sẻ tiện ích – Kết nối cộng đồng.")
                    .setPositiveButton("Đóng",null).show();
        }
        protected void logout(){
            confirm("Đăng xuất?","Bạn sẽ trở về màn hình đăng nhập.","Đăng xuất",()->{
                state.signedIn=false;vm.changed();host.reset(R.id.auth);
            });
        }
        protected View profileAvatar(Profile profile,int size){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(size),dp(size));
            if(profile.photo!=null){
                ImageView pic=image(Art.CAMERA,profile.photo,false);pic.setPadding(0,0,0,0);
                pic.setScaleType(ImageView.ScaleType.CENTER_CROP);pic.setBackground(shape(color(R.color.sb_tint),100));pic.setClipToOutline(true);
                pic.setLayoutParams(lp);pic.setContentDescription("Ảnh đại diện");return pic;
            }
            TextView p=avatar(initials(profile.name));p.setTextSize(size/3f);p.setLayoutParams(lp);return p;
        }
    }

    public static final class ProfilePage extends AccountPage {
        public ProfilePage(){super(R.layout.screen_profile);}
        @Override protected void render(){
            LinearLayout summary=panel();summary.setBackgroundResource(R.drawable.sb_gradient);
            LinearLayout row=row();row.addView(profileAvatar(state.profile,68));
            LinearLayout words=col();words.setPadding(dp(13),0,0,0);
            for(String label:new String[]{state.profile.name,state.profile.email,"★  4,8 · 24 đánh giá"}){
                TextView t=text(label,label.equals(state.profile.name)?20:12,R.color.sb_paper,label.equals(state.profile.name));
                t.setTextColor(WHITE);add(words,t,4);
            }
            weight(row,words);add(summary,row,0);
            add(summary,button("✎  Chỉnh sửa hồ sơ",false,()->open(R.id.edit_profile)),14);add(frame.content,summary,5);
            LinearLayout menu=panel();menu.setPadding(dp(5),0,dp(5),0);
            menu(menu,"◇","Sản phẩm của tôi","",0,()->open(R.id.my_products));
            menu(menu,"⇄","Giao dịch của tôi","",(int)state.transactions.stream().filter(t->t.stage()==Stage.PENDING).count(),()->open(R.id.transactions));
            menu(menu,"♡","Yêu thích","",0,()->open(R.id.wishlist));
            menu(menu,"◌","Chat của tôi","",state.chats.values().stream().mapToInt(c->c.unread).sum(),()->host.openTab(R.id.chats));
            menu(menu,"▣","Tài chính","",0,()->open(R.id.finance));
            menu(menu,"♧","Thông báo","",(int)state.notices.stream().filter(n->!n.read).count(),()->open(R.id.notifications));
            menu(menu,"⚙","Cài đặt","",0,()->open(R.id.settings));
            menu(menu,"ⓘ","Trợ giúp & hỗ trợ","",0,()->open(R.id.settings,1));
            menu(menu,"ⓘ","Về ShareBox","",0,this::about);
            menu(menu,"⇥","Đăng xuất","",0,this::logout);
            add(frame.content,menu,13);
            TextView version=meta("ShareBox 1.0.0");version.setGravity(Gravity.CENTER);add(frame.content,version,12);
        }
    }

    public static final class Edit extends AccountPage {
        private final ActivityResultLauncher<String> choosePhoto=registerForActivityResult(new ActivityResultContracts.GetContent(),uri->{
            if(uri!=null&&frame!=null){memory.putString("photo",uri.toString());refresh();}
        });
        private final ActivityResultLauncher<Void> camera=registerForActivityResult(new ActivityResultContracts.TakePicturePreview(),this::saveCamera);
        public Edit(){super(R.layout.screen_edit_profile);}
        @Override protected void render(){
            header();
            Profile saved=state.profile;
            String photo=memory.containsKey("photo")?memory.getString("photo"):saved.photo;
            LinearLayout portrait=row();portrait.setGravity(Gravity.CENTER);
            portrait.addView(profileAvatar(new Profile(memory.getString("name",saved.name),saved.email,"","","",photo),88));
            portrait.addView(button("✎",false,()->choose("Ảnh đại diện",new String[]{"Chọn từ thư viện","Chụp ảnh mới","Xóa ảnh hiện tại"},i->{
                if(i==0)choosePhoto.launch("image/*");
                else if(i==1){try{camera.launch(null);}catch(android.content.ActivityNotFoundException e){shortNote("Thiết bị chưa có ứng dụng máy ảnh.");}}
                else {memory.putString("photo",null);refresh();}
            })));
            add(frame.content,portrait,10);
            EditText name=editable("Họ và tên",memory.getString("name",saved.name),InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            EditText email=field(frame.content,"Email · Đã xác minh","",saved.email,InputType.TYPE_CLASS_TEXT,1);
            email.setKeyListener(null);email.setFocusable(false);
            add(frame.content,meta(EMAIL_NOTE),5);
            EditText phone=editable("Số điện thoại",memory.getString("phone",saved.phone),InputType.TYPE_CLASS_PHONE);
            EditText area=editable("Khu vực",memory.getString("area",saved.area),InputType.TYPE_CLASS_TEXT);
            EditText bio=field(frame.content,"Giới thiệu bản thân","Chưa cập nhật",memory.getString("bio",saved.bio),InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE,3);
            bio.setFilters(new InputFilter[]{new InputFilter.LengthFilter(150)});
            TextView remaining=meta(bio.length()+"/150");remaining.setGravity(Gravity.END);add(frame.content,remaining,3);
            footer("Lưu thay đổi",true,()->{
                boolean valid=true;
                if(value(name).isEmpty()){name.setError("Vui lòng nhập họ và tên");valid=false;}
                if(!value(phone).matches("0[0-9]{9}")){phone.setError("Số điện thoại gồm 10 chữ số, bắt đầu bằng 0");valid=false;}
                if(!valid)return;
                state.profile=new Profile(value(name),saved.email,value(phone),value(area),value(bio),photo);
                vm.changed();memory.clear();keyboardHide(name);shortNote("Đã lưu hồ sơ của bạn");refresh();
            });
            Runnable visibility=()->frame.footer.setVisibility(
                    value(name).equals(saved.name)&&value(phone).equals(saved.phone)&&value(area).equals(saved.area)&&
                            value(bio).equals(saved.bio)&&Objects.equals(photo,saved.photo)?View.GONE:View.VISIBLE);
            watch(name,s->{memory.putString("name",s);visibility.run();});
            watch(phone,s->{memory.putString("phone",s);visibility.run();});
            watch(area,s->{memory.putString("area",s);visibility.run();});
            watch(bio,s->{memory.putString("bio",s);remaining.setText(s.length()+"/150");visibility.run();});
            visibility.run();
        }
        private EditText editable(String label,String value,int type){
            EditText field=field(frame.content,label,"Chưa cập nhật",value,type,1);
            field.setFocusable(false);field.setFocusableInTouchMode(false);
            final boolean[] editing={false};
            TextView edit=button("✎  Sửa "+label.toLowerCase(Locale.ROOT),false,()->{});
            edit.setOnClickListener(v->{
                editing[0]=!editing[0];field.setFocusable(editing[0]);field.setFocusableInTouchMode(editing[0]);
                if(editing[0]){
                    field.requestFocus();field.setSelection(field.length());edit.setText("✓  Xong");
                    android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)requireContext()
                            .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if(keyboard!=null)keyboard.showSoftInput(field,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }else{keyboardHide(field);field.clearFocus();edit.setText("✎  Sửa "+label.toLowerCase(Locale.ROOT));}
            });
            add(frame.content,edit,2);
            return field;
        }
        private void saveCamera(Bitmap bmp){
            if(bmp==null||frame==null)return;
            try{
                File f=File.createTempFile("avatar-", ".jpg",requireContext().getCacheDir());
                try(FileOutputStream out=new FileOutputStream(f)){bmp.compress(Bitmap.CompressFormat.JPEG,90,out);}
                memory.putString("photo",Uri.fromFile(f).toString());refresh();
            }catch(IOException e){shortNote("Không thể mở ảnh đã chụp.");}
        }
    }

    public static final class Listings extends AccountPage {
        public Listings(){super(R.layout.screen_my_products);}
        @Override protected void render(){
            header();frame.header.addView(icon(R.drawable.sb_ic_plus,"Đăng tin mới",()->open(R.id.create_product)));
            int selected=memory.getInt("filter");
            List<Product> own=state.products.stream().filter(p->p.own).collect(Collectors.toList());
            String[] labels={"Tất cả "+own.size(),"Đang hiển thị","Đã ẩn","Đã gỡ"};
            for(Status s:Status.values())labels[s.ordinal()+1]+=" "+own.stream().filter(p->p.status==s).count();
            frame.above.setPadding(dp(16),0,dp(16),dp(5));
            chips(frame.above,labels,selected,i->{memory.putInt("filter",i);refresh();});
            List<Product> rows=own.stream().filter(p->selected==0||p.status.ordinal()+1==selected).collect(Collectors.toList());
            if(rows.isEmpty())empty(frame.content,"◇","Không có tin nào","Các tin ở trạng thái này sẽ hiển thị tại đây.","",null);
            else pageList(rows,1,this::card);
        }
        private View card(Product p){
            LinearLayout card=panel();
            LinearLayout top=row();
            weight(top,tag(p.status.label,p.status==Status.ACTIVE?R.color.sb_success:R.color.sb_muted));
            if(p.recent())top.addView(tag("NEW",R.color.sb_accent));
            if(p.hot()){ImageView hot=new ImageView(requireContext());hot.setImageResource(R.drawable.hot_art);hot.setContentDescription("HOT");top.addView(hot,new LinearLayout.LayoutParams(dp(38),dp(38)));}
            add(card,top,0);
            add(card,productSummary(p,p.price()+(p.note.isEmpty()?"":" · "+p.note)),7);
            if(p.status==Status.REMOVED){card.setAlpha(.65f);return card;}
            LinearLayout action=row();
            weight(action,button("✎  Sửa",false,()->open(R.id.create_product,p.id)));
            weight(action,button(p.status==Status.ACTIVE?"Ẩn tin":"Hiện tin",false,()->{
                vm.listing(p.status(p.status==Status.ACTIVE?Status.HIDDEN:Status.ACTIVE));shortNote(p.status==Status.ACTIVE?"Đã ẩn tin":"Đã hiện tin");refresh();
            }));
            TextView delete=button("×",false,()->confirm("Gỡ tin?","Tin sẽ biến mất khỏi Khám phá.","Gỡ tin",()->{
                vm.listing(p.status(Status.REMOVED));shortNote("Đã gỡ tin");refresh();
            }));
            delete.setTextColor(color(R.color.sb_error));delete.setContentDescription("Xóa tin");action.addView(delete);
            add(card,action,9);return card;
        }
    }

    public static final class Create extends AccountPage {
        private final ActivityResultLauncher<String> photos=registerForActivityResult(new ActivityResultContracts.GetMultipleContents(),uris->{
            if(frame==null||uris.isEmpty())return;
            ArrayList<String> files=images();
            for(Uri u:uris)if(files.size()<8&&!files.contains(u.toString()))files.add(u.toString());
            memory.putStringArrayList("photos",files);refresh();
        });
        private AlertDialog discardDialog;
        public Create(){super(R.layout.screen_create_product);}
        @Override public void onCreate(Bundle saved){
            super.onCreate(saved);
            requireActivity().getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
                @Override public void handleOnBackPressed(){back();}
            });
        }
        @Override protected void back(){requestLeave(super::back);}
        public void requestLeave(Runnable leave){
            if(!hasUnsavedChanges()){
                leave.run();
                return;
            }
            if(discardDialog!=null)return;
            discardDialog=new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.sb_discard_listing_title)
                    .setMessage(R.string.sb_discard_listing_message)
                    .setNegativeButton(R.string.sb_keep_editing,null)
                    .setPositiveButton(R.string.sb_discard_listing,(dialog,which)->leave.run())
                    .create();
            discardDialog.setOnDismissListener(dialog->discardDialog=null);
            discardDialog.show();
        }
        @Override public void onDestroyView(){
            if(discardDialog!=null)discardDialog.dismiss();
            super.onDestroyView();
        }
        private boolean hasUnsavedChanges(){
            if(memory.getBoolean("saved"))return false;
            Product p=product();
            boolean initiallyFree=p!=null&&p.rate==0;
            boolean free=memory.getBoolean("free",initiallyFree);
            String savedCondition=memory.getString("listing_condition");
            return textChanged("name",p==null?"":p.name)
                    ||textChanged("description",p==null?"":p.description)
                    ||textChanged("area",p==null?state.profile.area:p.location)
                    ||textChanged("quantity",p==null?"1":Integer.toString(p.quantity))
                    ||textChanged("deposit",p==null||p.deposit==0?"":Long.toString(p.deposit))
                    ||free!=initiallyFree
                    ||(!free&&textChanged("price",p==null||p.rate==0?"":Long.toString(p.rate)))
                    ||memory.getInt("category",initialCategory(p))!=initialCategory(p)
                    ||(savedCondition!=null&&!Condition.valueOf(savedCondition).label.equals(p==null?Condition.NEW.label:p.condition))
                    ||!images().equals(p==null?Collections.emptyList():p.images);
        }
        private boolean textChanged(String key,String initial){
            return !memory.getString(key,initial).trim().equals(initial.trim());
        }
        private int initialCategory(Product p){
            return p==null?0:Math.max(0,Arrays.asList(CATEGORIES).indexOf(p.category));
        }
        private ArrayList<String> images(){
            ArrayList<String> value=memory.getStringArrayList("photos");
            Product p=product();
            return value!=null?new ArrayList<>(value):new ArrayList<>(p==null?Collections.emptyList():p.images);
        }
        @Override protected void render(){
            Product p=product();
            header(p==null?screenTitle():"Chỉnh sửa tin");
            if(memory.getBoolean("saved")){
                note(frame.content,p==null?"Đăng tin thành công! Tin của bạn đang hiển thị trong Khám phá.":"Đã lưu thay đổi của sản phẩm.",R.color.sb_success);
                footer("Xem sản phẩm của tôi",true,()->open(R.id.my_products));
                return;
            }
            LinearLayout view=frame.content;
            add(view,title("Hình ảnh (tối đa 8 ảnh)"),0);
            gallery(p);
            EditText name=draft(view,"Tên sản phẩm","Ví dụ: Máy ảnh Canon EOS 200D","name",p==null?"":p.name,InputType.TYPE_CLASS_TEXT,1);
            TextView nameError=validationMessage(view,name);
            EditText desc=draft(view,"Mô tả","Tình trạng, phụ kiện đi kèm, lưu ý khi sử dụng…","description",p==null?"":p.description,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE,3);
            int category=memory.getInt("category",initialCategory(p));
            Condition previous=p==null?Condition.NEW:Condition.fromStored(p.condition);
            String savedCondition=memory.getString("listing_condition");
            int condition=savedCondition==null?(previous==null?-1:previous.ordinal()):
                    Condition.valueOf(savedCondition).ordinal();
            add(view,body("Danh mục"),14);chips(view,CATEGORIES,category,i->{memory.putInt("category",i);refresh();});
            add(view,body("Tình trạng sản phẩm"),14);
            chips(view,CONDITION_LABELS,condition,i->{memory.putString("listing_condition",CONDITIONS[i].name());refresh();});
            EditText area=draft(view,"Khu vực","Thủ Đức, TP.HCM","area",p==null?state.profile.area:p.location,InputType.TYPE_CLASS_TEXT,1);
            LinearLayout fees=panel();add(fees,title("Thuê / mượn"),0);
            boolean free=memory.getBoolean("free",p!=null&&p.rate==0);
            Switch type=new Switch(requireContext());type.setText("Cho mượn miễn phí");type.setTextColor(color(R.color.sb_ink));
            type.setMinHeight(dp(48));type.setChecked(free);
            type.setOnCheckedChangeListener((b,v)->{memory.putBoolean("free",v);refresh();});
            add(fees,type,7);
            if(free)add(fees,text("Miễn phí",15,R.color.sb_success,true),4);
            EditText price=free?null:draft(fees,"Giá thuê mỗi ngày (đ)","250000","price",p==null||p.rate==0?"":""+p.rate,InputType.TYPE_CLASS_NUMBER,1);
            TextView priceError=price==null?null:validationMessage(fees,price);
            EditText deposit=draft(fees,"Tiền đặt cọc (tùy chọn)","Không cần cọc","deposit",p==null||p.deposit==0?"":""+p.deposit,InputType.TYPE_CLASS_NUMBER,1);
            add(fees,meta("Tiền cọc hiển thị riêng, hoàn lại khi trả đồ."),6);
            EditText quantity=draft(fees,"Số lượng","1","quantity",p==null?"1":""+p.quantity,InputType.TYPE_CLASS_NUMBER,1);
            TextView quantityError=validationMessage(fees,quantity);
            if(price!=null)price.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});
            deposit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});
            quantity.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
            add(view,fees,14);
            footer(p==null?"Đăng tin":"Lưu thay đổi",true,()->{
                boolean invalidName=value(name).isEmpty();
                boolean invalidPrice=price!=null&&number(price)<=0;
                boolean invalidQuantity=number(quantity)<=0||number(quantity)>999;
                showValidation(nameError,invalidName?"Vui lòng nhập tên sản phẩm":null);
                if(priceError!=null)showValidation(priceError,invalidPrice?"Nhập giá thuê mỗi ngày":null);
                showValidation(quantityError,invalidQuantity?"Nhập số lượng từ 1 đến 999":null);
                if(invalidName||invalidPrice||invalidQuantity){
                    EditText first=invalidName?name:invalidPrice?price:quantity;
                    TextView message=invalidName?nameError:invalidPrice?priceError:quantityError;
                    first.requestFocus();
                    message.post(()->{
                        if(message.isAttachedToWindow()){
                            first.requestRectangleOnScreen(new Rect(0,0,first.getWidth(),
                                    first.getHeight()+message.getHeight()+dp(4)),true);
                        }
                    });
                    return;
                }
                Art art=p==null?art(CATEGORIES[category]):p.art;
                Product result=new Product(p==null?vm.nextProduct():p.id,value(name),art,price==null?0:number(price),number(deposit),
                        value(area).isEmpty()?state.profile.area:value(area),(int)number(quantity),p==null?state.profile.name:p.owner,
                        CATEGORIES[category],p!=null&&savedCondition==null?p.condition:CONDITIONS[condition].label,
                        value(desc),true,p==null?Status.ACTIVE:p.status,
                        p==null?DEMO:p.posted,p==null?0:p.goodReviews,p==null?0:p.rentals,images(),p==null?"":p.note);
                keyboardHide(name);vm.listing(result);memory.putBoolean("saved",true);refresh();
            });
        }
        private TextView validationMessage(LinearLayout parent,EditText field){
            TextView message=text("",12,R.color.sb_error,false);
            message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            message.setVisibility(View.GONE);
            add(parent,message,4);
            watch(field,value->showValidation(message,null));
            return message;
        }
        private void showValidation(TextView message,String error){
            message.setText(error);
            message.setVisibility(error==null?View.GONE:View.VISIBLE);
        }
        private EditText draft(LinearLayout parent,String label,String hint,String key,String fallback,int input,int lines){
            EditText field=field(parent,label,hint,memory.getString(key,fallback),input,lines);
            watch(field,s->memory.putString(key,s));return field;
        }
        private Art art(String category){
            switch(category){
                case "Laptop":case "Điện thoại":return Art.LAPTOP;
                case "Đồ gia dụng":case "Dụng cụ":return Art.DRILL;
                case "Thể thao":return Art.BIKE;
                case "Sách":return Art.BOOK;
                case "Nội thất":return Art.CHAIR;
                default:return Art.CAMERA;
            }
        }
        private void gallery(Product p){
            ArrayList<String> photos=images();
            HorizontalScrollView scroll=new HorizontalScrollView(requireContext());scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout row=row();
            if(photos.size()<8)row.addView(button("▧\nThêm ảnh",false,()->this.photos.launch("image/*")),new LinearLayout.LayoutParams(dp(85),dp(82)));
            if(photos.isEmpty()&&p==null){
                addPhoto(row,Art.CAMERA,null,"Ảnh bìa (mẫu)",null);
                addPhoto(row,Art.LAPTOP,null,"Ảnh minh họa",null);
            }
            for(int i=0;i<photos.size();i++){
                final int index=i;
                addPhoto(row,p==null?Art.CAMERA:p.art,photos.get(i),i==0?"Ảnh bìa":"Ảnh "+(i+1),()->{
                    choose("Ảnh sản phẩm",new String[]{"Đặt làm ảnh bìa","Xóa ảnh"},choice->{
                        String selected=photos.remove(index);
                        if(choice==0)photos.add(0,selected);
                        memory.putStringArrayList("photos",photos);refresh();
                    });
                });
            }
            scroll.addView(row);add(frame.content,scroll,6);
        }
        private void addPhoto(LinearLayout row,Art art,String uri,String label,Runnable action){
            FrameLayout thumbnail=new FrameLayout(requireContext());
            ImageView img=image(art,uri,false);img.setContentDescription(label);
            thumbnail.addView(img,new FrameLayout.LayoutParams(-1,-1));
            TextView tag=tag(label,R.color.sb_accent);
            thumbnail.addView(tag,new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START));
            if(action!=null)thumbnail.setOnClickListener(v->action.run());
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(86),dp(82));lp.leftMargin=dp(8);row.addView(thumbnail,lp);
        }
    }

    public static final class Finance extends AccountPage {
        public Finance(){super(R.layout.screen_finance);}
        @Override protected void render(){
            header();
            int period=memory.getInt("period"),filter=memory.getInt("filter");
            chips(frame.content,new String[]{"Tháng này","Quý này","Năm nay"},period,i->{memory.putInt("period",i);refresh();});
            LocalDate from=period==0?DEMO.withDayOfMonth(1):period==1?LocalDate.of(DEMO.getYear(),10,1):DEMO.withDayOfYear(1);
            List<Entry> entries=state.entries.stream().filter(e->!e.date.isBefore(from)).sorted(Comparator.comparing((Entry e)->e.date).reversed()).collect(Collectors.toList());
            long income=sum(entries,EntryType.INCOME), expense=-sum(entries,EntryType.EXPENSE);
            LinearLayout balance=panel();balance.setBackgroundResource(R.drawable.sb_gradient);
            LinearLayout row=row();weight(row,whiteTotal("↗  Tổng thu",money(income)));weight(row,whiteTotal("↙  Tổng chi",money(expense)));
            add(balance,row,0);add(balance,whiteTotal("Chênh lệch thu – chi",(income>=expense?"+":"")+money(income-expense)),13);
            add(frame.content,balance,12);
            add(frame.content,title("Tiền cọc"),13);add(frame.content,meta("Tách riêng, không tính là thu nhập"),4);
            LinearLayout totals=row();
            weight(totals,deposit("Đang giữ",money(sum(entries,EntryType.HELD)),R.color.sb_warning));
            weight(totals,deposit("Đã hoàn",money(-sum(entries,EntryType.REFUND)),R.color.sb_success));
            weight(totals,deposit("Khấu trừ",money(-sum(entries,EntryType.DEDUCTION)),R.color.sb_error));
            add(frame.content,totals,9);
            chart();
            add(frame.content,title("Lịch sử"),15);
            chips(frame.content,new String[]{"Tất cả","Thu","Chi","Cọc"},filter,i->{memory.putInt("filter",i);refresh();});
            List<Entry> history=entries.stream().filter(e->filter==0||
                    (filter==1&&e.type==EntryType.INCOME)||(filter==2&&e.type==EntryType.EXPENSE)||(filter==3&&e.type.deposit())).collect(Collectors.toList());
            if(history.isEmpty())empty(frame.content,"▣","Chưa có dữ liệu","Lịch sử sẽ xuất hiện khi có giao dịch phù hợp.","",null);
            else embedded(frame.content,history,1,this::entryRow);
            add(frame.content,meta("Đây là sổ theo dõi thu chi, không phải ví điện tử. ShareBox không giữ hay chuyển tiền."),10);
        }
        private long sum(List<Entry> rows,EntryType type){return rows.stream().filter(e->e.type==type).mapToLong(e->e.amount).sum();}
        private LinearLayout whiteTotal(String caption,String money){
            LinearLayout col=col();TextView name=meta(caption);name.setTextColor(0xffe0eaff);add(col,name,0);
            TextView amount=title(money);amount.setTextColor(WHITE);add(col,amount,4);return col;
        }
        private View deposit(String title,String value,int tone){
            LinearLayout card=panel();card.setPadding(dp(9),dp(10),dp(6),dp(10));add(card,meta(title),0);add(card,text(value,14,tone,true),5);return card;
        }
        private void chart(){
            LinearLayout box=panel();add(box,title("Thu chi theo tháng"),0);add(box,meta("Triệu đồng"),5);
            LinearLayout data=row();data.setGravity(Gravity.BOTTOM);
            long max=1;
            long[] in=new long[6],out=new long[6];
            for(int i=0;i<6;i++){
                final int month=DEMO.minusMonths(6-i).getMonthValue();
                List<Entry> m=state.entries.stream().filter(e->e.date.getMonthValue()==month&&e.date.getYear()==DEMO.getYear()).collect(Collectors.toList());
                in[i]=sum(m,EntryType.INCOME);out[i]=-sum(m,EntryType.EXPENSE);max=Math.max(max,Math.max(in[i],out[i]));
            }
            for(int i=0;i<6;i++){
                LinearLayout m=col();m.setGravity(Gravity.CENTER_HORIZONTAL);
                LinearLayout bars=row();bars.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);
                bar(bars,in[i],max,0xff265de6,"Thu");bar(bars,out[i],max,0xffad8a55,"Chi");
                m.addView(bars,new LinearLayout.LayoutParams(-1,dp(118)));
                TextView label=meta("T"+DEMO.minusMonths(6-i).getMonthValue());label.setGravity(Gravity.CENTER);add(m,label,6);
                weight(data,m);
            }
            add(box,data,8);add(box,meta("● Thu     ● Chi"),10);add(frame.content,box,13);
        }
        private void bar(LinearLayout col,long value,long max,int color,String label){
            View bar=new View(requireContext());bar.setBackground(shape(color,4));bar.setContentDescription(label+" "+money(value));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(11),dp(Math.max(2,value*108f/max)));lp.leftMargin=dp(3);col.addView(bar,lp);
        }
        private View entryRow(Entry e){
            LinearLayout card=panel(),row=row();
            LinearLayout text=col();add(text,body(e.title),0);add(text,meta(date(e.date)+" · "+e.type.label),4);weight(row,text);
            row.addView(text((e.amount>=0?"+":"")+money(e.amount),13,e.amount>=0?R.color.sb_success:R.color.sb_warning,true));
            add(card,row,0);return card;
        }
    }

    public static final class Notices extends AccountPage {
        public Notices(){super(R.layout.screen_notifications);}
        @Override protected void render(){
            header();
            TextView read=button("✓✓",false,()->{
                state.notices.forEach(n->n.read=true);vm.changed();shortNote("Đã đánh dấu tất cả đã đọc");refresh();
            });
            read.setContentDescription("Đánh dấu tất cả đã đọc");frame.header.addView(read);
            int filter=memory.getInt("filter");
            String[] labels=new String[NoticeType.values().length+1];
            labels[0]=label("Tất cả",state.notices.stream().filter(n->!n.read).count());
            for(NoticeType t:NoticeType.values())labels[t.ordinal()+1]=label(t.label,state.notices.stream().filter(n->!n.read&&n.type==t).count());
            frame.above.setPadding(dp(16),0,dp(16),dp(5));
            chips(frame.above,labels,filter,i->{memory.putInt("filter",i);refresh();});
            List<Notice> rows=state.notices.stream().filter(n->filter==0||n.type.ordinal()+1==filter).collect(Collectors.toList());
            if(rows.isEmpty())empty(frame.content,"♧","Chưa có thông báo","Thông báo về tin nhắn và giao dịch sẽ xuất hiện ở đây.","",null);
            else pageList(rows,1,this::card);
        }
        private String label(String text,long unread){return text+(unread>0?"  "+unread:"");}
        private View card(Notice n){
            int bg=n.read?R.drawable.sb_card:R.drawable.sb_tint;
            LinearLayout card=panel();LinearLayout title=row();
            weight(title,text(n.title,14,R.color.sb_ink,true));title.addView(meta(n.time+(n.read?"":"  ●")));
            add(card,title,0);add(card,meta(n.body),6);
            String action=n.type==NoticeType.CHAT?"Mở cuộc trò chuyện":n.type==NoticeType.PAYMENT?"Xem thanh toán":n.type==NoticeType.REQUEST?"Xem yêu cầu":"Xem giao dịch";
            add(card,text(action,12,R.color.sb_accent,true),7);
            click(card,bg,()->{
                n.read=true;vm.changed();
                switch(n.type){
                    case CHAT:Product p=state.product(n.target);if(p!=null)openChat(p);break;
                    case REQUEST:open(R.id.transactions);break;
                    case PAYMENT:Bundle b=new Bundle();b.putInt("id",n.target);b.putBoolean("notice",true);host.open(R.id.payment,b);break;
                    default:open(R.id.tx_detail,n.target);
                }
            });
            return card;
        }
    }

    public static final class Settings extends AccountPage {
        public Settings(){super(R.layout.screen_settings);}
        @Override protected void render(){
            header();
            section("GIAO DIỆN",p->{
                switchRow(p,"Chế độ tối","Giao diện sáng / tối",state.dark,host::dark);
                switchRow(p,getString(R.string.sb_ai_home_setting),getString(R.string.sb_ai_home_description),
                        host.showsHomeAiShortcut(),host::setHomeAiShortcut);
            });
            section("TÀI KHOẢN",p->{
                menu(p,"♙","Thông tin tài khoản","",0,()->open(R.id.edit_profile));
                menu(p,"♧","Đổi mật khẩu","",0,this::changePassword);
                menu(p,"⌖","Địa chỉ của tôi",state.profile.area+" · Mặc định",0,this::address);
            });
            section("THÔNG BÁO",p->{
                for(String name:new String[]{"Tin nhắn mới","Yêu cầu & trạng thái giao dịch","Xác nhận thanh toán","Nhắc bàn giao và trả đồ"})pref(p,name,"");
            });
            section("QUYỀN RIÊNG TƯ",p->{
                pref(p,"Hiển thị số điện thoại","Chỉ hiện với người đã có giao dịch");
                menu(p,"◎","Quản lý quyền riêng tư","",0,this::privacy);
            });
            section("HỖ TRỢ",p->{
                menu(p,"?","Trung tâm trợ giúp","",0,this::help);
                menu(p,"!","Báo cáo sự cố","",0,this::report);
                menu(p,"ⓘ","Về ShareBox","Phiên bản 1.0.0",0,this::about);
                menu(p,"◌","Trạng thái danh sách","",0,()->open(R.id.states));
            });
            add(frame.content,button("Đăng xuất",false,this::logout),15);
            if(itemId()!=0&&!memory.getBoolean("opened")){
                memory.putBoolean("opened",true);
                frame.content.post(()->{if(frame!=null){if(itemId()==1)help();else about();}});
            }
        }
        private void section(String title,java.util.function.Consumer<LinearLayout> content){
            add(frame.content,meta(title),12);
            LinearLayout section=panel();section.setPadding(dp(6),dp(3),dp(6),dp(3));
            content.accept(section);add(frame.content,section,6);
        }
        private void pref(LinearLayout card,String name,String description){
            switchRow(card,name,description,state.preferences.getOrDefault(name,false),on->{state.preferences.put(name,on);vm.changed();});
        }
        private void switchRow(LinearLayout card,String name,String description,boolean enabled,java.util.function.Consumer<Boolean> change){
            LinearLayout line=row();line.setPadding(dp(9),0,dp(9),0);line.setMinimumHeight(dp(54));
            LinearLayout text=col();add(text,body(name),0);if(!description.isEmpty())add(text,meta(description),3);weight(line,text);
            androidx.appcompat.widget.SwitchCompat toggle=new androidx.appcompat.widget.SwitchCompat(requireContext());
            toggle.setContentDescription(name);toggle.setChecked(enabled);toggle.setMinHeight(dp(48));
            toggle.setOnCheckedChangeListener((button,on)->change.accept(on));line.addView(toggle);
            add(card,line,0);
        }
        private LinearLayout dialogBody(){
            LinearLayout v=col();v.setPadding(dp(16),0,dp(16),dp(12));return v;
        }
        private void help(){
            String[] questions={"Thuê hoặc mượn đồ","Thanh toán và đặt cọc","Bàn giao và hoàn trả"};
            String[] answers={"Tìm sản phẩm, chat với chủ đồ và gửi yêu cầu khi đã thống nhất.",
                    EXTERNAL_PAYMENT,"Hai bên cần xác nhận bàn giao và xác nhận trả đồ. Nếu không thống nhất, báo tranh chấp trong chi tiết giao dịch."};
            choose("Trung tâm trợ giúp",questions,i->new AlertDialog.Builder(requireContext()).setTitle(questions[i])
                    .setMessage(answers[i]).setPositiveButton("Đóng",null).show());
        }
        private void report(){
            LinearLayout v=dialogBody();EditText input=field(v,"Mô tả sự cố","Nhập nội dung","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE,3);
            validatedDialog("Báo cáo sự cố",v,"Gửi",()->{
                if(value(input).isEmpty()){input.setError("Vui lòng nhập mô tả");return false;}
                state.reports.add(value(input));vm.changed();shortNote("Đã gửi báo cáo mẫu");return true;
            });
        }
        private void address(){
            LinearLayout v=dialogBody();EditText area=field(v,"Địa chỉ mặc định","Thủ Đức, TP.HCM",state.profile.area,InputType.TYPE_CLASS_TEXT,1);
            validatedDialog("Địa chỉ của tôi",v,"Lưu",()->{
                if(value(area).isEmpty()){area.setError("Vui lòng nhập địa chỉ");return false;}
                Profile p=state.profile;state.profile=new Profile(p.name,p.email,p.phone,value(area),p.bio,p.photo);
                vm.changed();refresh();return true;
            });
        }
        private void changePassword(){
            LinearLayout v=dialogBody();
            int password=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD;
            EditText old=field(v,"Mật khẩu hiện tại","Tối thiểu 6 ký tự","",password,1);
            EditText next=field(v,"Mật khẩu mới","Tối thiểu 6 ký tự","",password,1);
            EditText again=field(v,"Xác nhận mật khẩu","Nhập lại mật khẩu mới","",password,1);
            validatedDialog("Đổi mật khẩu",v,"Lưu",()->{
                if(value(old).length()<6){old.setError("Mật khẩu cần ít nhất 6 ký tự");return false;}
                if(value(next).length()<6){next.setError("Mật khẩu cần ít nhất 6 ký tự");return false;}
                if(!value(next).equals(value(again))){again.setError("Mật khẩu xác nhận không khớp");return false;}
                shortNote("Đã mô phỏng đổi mật khẩu");return true;
            });
        }
        private void privacy(){
            LinearLayout v=dialogBody();
            add(v,meta("Số điện thoại chỉ có thể xem với người đã có giao dịch."),5);
            pref(v,"Hiển thị số điện thoại","");
            if(!state.blocked.isEmpty()){
                add(v,title("Người dùng bị chặn"),9);
                for(String name:new ArrayList<>(state.blocked)){
                    add(v,button("Bỏ chặn "+name,false,()->{state.blocked.remove(name);vm.changed();shortNote("Đã bỏ chặn");}),4);
                }
            }
            new AlertDialog.Builder(requireContext()).setTitle("Quản lý quyền riêng tư").setView(v).setPositiveButton("Đóng",null).show();
        }
        private void validatedDialog(String title,LinearLayout view,String action,java.util.function.BooleanSupplier save){
            AlertDialog dialog=new AlertDialog.Builder(requireContext()).setTitle(title).setView(view)
                    .setNegativeButton("Hủy",null).setPositiveButton(action,null).create();
            dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(save.getAsBoolean())dialog.dismiss();}));
            dialog.show();
        }
    }
}
