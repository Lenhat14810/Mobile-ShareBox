package com.example.sharebox.ui;

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import com.example.sharebox.R;
import androidx.activity.OnBackPressedCallback;
import com.example.sharebox.databinding.ComponentAuthHeaderBinding;
import com.example.sharebox.databinding.ComponentPasswordBinding;

public final class AuthPages {
    private AuthPages(){}
    public static final class Splash extends Page {
        private final Handler timer=new Handler(Looper.getMainLooper());
        private int value;
        private boolean moved;
        public Splash(){super(R.layout.screen_splash);}
        @Override protected void render(){
            View root=requireView().findViewById(R.id.splash_content);
            ProgressBar bar=root.findViewById(R.id.splash_progress);
            TextView hint=root.findViewById(R.id.splash_hint);
            root.setOnClickListener(v->next());
            timer.post(new Runnable(){
                @Override public void run(){
                    if(frame==null||moved)return;
                    value=Math.min(100,value+(value<65?8:4));
                    bar.setProgress(value);
                    hint.setText(value+"%  ·  Đang tải\nChạm để tiếp tục");
                    if(value<100)timer.postDelayed(this,80); else timer.postDelayed(()->next(),300);
                }
            });
        }
        private void next(){if(frame!=null&&!moved){moved=true;host.reset(R.id.onboarding);}}
        @Override public void onDestroyView(){timer.removeCallbacksAndMessages(null);super.onDestroyView();}
    }

    public static final class Onboarding extends Page {
        private static final String[] HEAD={"Khám phá đồ dùng","Kết nối dễ dàng","Chia sẻ thông minh"};
        private static final String[] BODY={"Tìm kiếm hàng nghìn món đồ có thể thuê hoặc mượn ngay gần bạn.",
                "Trò chuyện với chủ đồ và thống nhất thời gian, phí thuê và tiền cọc.",
                "Cho thuê hoặc cho mượn đồ ít dùng và giúp cộng đồng tiết kiệm."};
        public Onboarding(){super(R.layout.screen_onboarding);}
        @Override protected void render(){
            int index=memory.getInt("page");
            frame.header.setVisibility(View.VISIBLE);
            ImageView mark=new ImageView(requireContext());mark.setImageResource(R.drawable.ic_sharebox_mark);
            mark.setBackgroundResource(R.drawable.sb_blue);mark.setPadding(dp(6),dp(6),dp(6),dp(6));
            frame.header.addView(mark,new LinearLayout.LayoutParams(dp(35),dp(35)));
            TextView brand=title("ShareBox");brand.setPadding(dp(9),0,0,0);weight(frame.header,brand);
            frame.header.addView(button("Bỏ qua",false,()->open(R.id.auth)));
            FrameLayout illustration=new FrameLayout(requireContext());
            if(index==0){
                ImageView art=new ImageView(requireContext());art.setImageResource(R.drawable.welcome_art);art.setScaleType(ImageView.ScaleType.FIT_CENTER);
                illustration.addView(art,new FrameLayout.LayoutParams(-1,-1));
            } else {
                LinearLayout sample=panel();sample.setBackgroundResource(index==1?R.drawable.sb_tint:R.drawable.sb_gradient);
                if(index==1){
                    note(sample,"Chào bạn, máy ảnh Canon còn trống nhé!",R.color.sb_ink);
                    TextView reply=body("Mình muốn thuê trong ba ngày.");
                    reply.setBackgroundResource(R.drawable.sb_blue);reply.setTextColor(WHITE);
                    reply.setPadding(dp(10),dp(10),dp(10),dp(10));add(sample,reply,10);
                    note(sample,"Bạn có thể nhận máy vào ngày 8/12.",R.color.sb_ink);
                } else {
                    for(String line:new String[]{"Tiết kiệm tháng này","1.250.000đ","12 món được mượn   ★ 4,9"}){
                        TextView text=title(line);text.setTextColor(WHITE);add(sample,text,12);
                    }
                }
                FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);lp.setMargins(dp(26),0,dp(26),0);
                illustration.addView(sample,lp);
            }
            illustration.setOnTouchListener(new View.OnTouchListener() {
                float start;
                @Override public boolean onTouch(View v,MotionEvent event){
                    if(event.getActionMasked()==MotionEvent.ACTION_DOWN){start=event.getX();return true;}
                    if(event.getActionMasked()==MotionEvent.ACTION_UP){
                        float dx=event.getX()-start;
                        if(Math.abs(dx)>dp(44))select(Math.max(0,Math.min(2,index+(dx<0?1:-1))));
                        return true;
                    }
                    return true;
                }
            });
            int height=Math.max(230,Math.min(460,getResources().getConfiguration().screenHeightDp-370));
            frame.content.addView(illustration,new LinearLayout.LayoutParams(-1,dp(height)));
            TextView heading=text(HEAD[index],25,R.color.sb_ink,true);heading.setGravity(Gravity.CENTER);add(frame.content,heading,14);
            TextView desc=meta(BODY[index]);desc.setGravity(Gravity.CENTER);add(frame.content,desc,8);
            LinearLayout dots=row();dots.setGravity(Gravity.CENTER);
            for(int i=0;i<3;i++){
                int page=i;TextView d=button(i==index?"━":"•",false,()->select(page));d.setTextColor(color(i==index?R.color.sb_accent:R.color.sb_muted));
                d.setContentDescription("Trang "+(i+1));dots.addView(d);
            }
            add(frame.content,dots,14);
            add(frame.content,button(index==2?"Bắt đầu ngay":"Tiếp tục   ›",true,()->{if(index==2)open(R.id.auth);else select(index+1);}),8);
            add(frame.content,button("Đăng nhập",false,()->open(R.id.auth)),8);
        }
        void select(int page){memory.putInt("page",page);refresh();}
    }

    public static final class Auth extends Page {
        private boolean submitting;
        private OnBackPressedCallback backToLogin;
        public Auth(){super(R.layout.screen_auth);}
        @Override protected void render(){
            int mode=memory.getInt("mode");
            if(backToLogin==null){
                backToLogin=new OnBackPressedCallback(mode!=0){
                    @Override public void handleOnBackPressed(){mode(0);}
                };
                host.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),backToLogin);
            }
            backToLogin.setEnabled(mode!=0);
            boolean registration=mode==1, recover=mode==2;
            if(mode!=0){
                frame.header.setVisibility(View.VISIBLE);
                frame.header.addView(icon(R.drawable.sb_ic_back,"Về đăng nhập",()->mode(0)));
            }
            ComponentAuthHeaderBinding brand=ComponentAuthHeaderBinding.inflate(getLayoutInflater(),frame.above,false);
            brand.authTitle.setText(registration?"Tạo tài khoản":recover?"Quên mật khẩu":"Chào mừng trở lại");
            brand.authSubtitle.setText(registration?"Bắt đầu chia sẻ cùng ShareBox":recover?"Nhập email đăng nhập của bạn":"Đăng nhập để tiếp tục chia sẻ");
            frame.above.addView(brand.getRoot());
            EditText name=registration?field(frame.content,"Họ và tên","Nguyễn Minh Anh","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS,1):null;
            EditText email=field(frame.content,"Email","minhanh@gmail.com",memory.getString("email",""),InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,1);
            watch(email,s->memory.putString("email",s));
            EditText pass=recover?null:password("Mật khẩu");
            EditText again=registration?password("Xác nhận mật khẩu"):null;
            if(again!=null)watch(again,s->{if(!s.isEmpty()&&!s.equals(value(pass)))again.setError("Mật khẩu xác nhận không khớp");else again.setError(null);});
            if(!registration&&!recover)add(frame.content,link("Quên mật khẩu?",Gravity.END,()->mode(2)),4);
            TextView feedback=text("",13,R.color.sb_success,true);
            TextView submit=button(registration?"Đăng ký":recover?"Gửi liên kết":"Đăng nhập",true,()->{
                boolean ok=true;
                if(name!=null && value(name).isEmpty()){name.setError("Vui lòng nhập họ và tên");ok=false;}
                if(!Patterns.EMAIL_ADDRESS.matcher(value(email)).matches()){email.setError("Email không hợp lệ, ví dụ: ten@gmail.com");ok=false;}
                if(pass!=null&&value(pass).length()<6){pass.setError("Mật khẩu cần ít nhất 6 ký tự");ok=false;}
                if(again!=null&&(value(again).isEmpty()||!value(again).equals(value(pass)))){
                    again.setError(value(again).isEmpty()?"Vui lòng nhập lại mật khẩu":"Mật khẩu xác nhận không khớp");ok=false;
                }
                if(!ok||submitting)return;
                keyboardHide(email);
                if(recover){
                    feedback.setText("Đã mô phỏng gửi liên kết đặt lại mật khẩu tới "+value(email)+".");
                    return;
                }
                submitting=true;
                feedback.setText((registration?"Đăng ký":"Đăng nhập")+" thành công! Đang vào Trang chủ…");
                feedback.setOnClickListener(v->{if(state.signedIn&&isAdded())host.reset(R.id.home);});
                vm.authenticate(value(email),name==null?"":value(name),value(pass),registration,()->{
                    if(frame!=null&&isAdded())host.reset(R.id.home);
                });
            });
            add(frame.content,submit,14);add(frame.content,feedback,10);
            if(mode==0){
                TextView or=meta("────────       hoặc       ────────");or.setGravity(Gravity.CENTER);add(frame.content,or,10);
                add(frame.content,button("G   Tiếp tục với Google",false,()->{
                    if(submitting)return;
                    submitting=true;feedback.setText("Đăng nhập mô phỏng thành công! Đang vào Trang chủ…");
                    vm.authenticate("","","",false,()->{if(frame!=null&&isAdded())host.reset(R.id.home);});
                }),12);
            }
            add(frame.content,link(mode==0?"Chưa có tài khoản? Đăng ký":"Đã có tài khoản? Đăng nhập",Gravity.CENTER,
                    ()->mode(mode==0?1:0)),14);
        }
        private TextView link(String label,int gravity,Runnable action){
            TextView link=text(label,14,R.color.sb_ink,true);
            link.setGravity(gravity|Gravity.CENTER_VERTICAL);link.setMinHeight(dp(48));
            click(link,0,action);return link;
        }
        private EditText password(String title){
            ComponentPasswordBinding form=ComponentPasswordBinding.inflate(getLayoutInflater(),frame.content,false);
            form.passwordLabel.setText(title);
            form.passwordInput.setTypeface(android.graphics.Typeface.DEFAULT);
            EditText p=form.passwordInput;
            final boolean[] shown={false};
            form.passwordVisibility.setOnClickListener(v->{
                shown[0]=!shown[0];p.setTransformationMethod(shown[0]?HideReturnsTransformationMethod.getInstance():PasswordTransformationMethod.getInstance());
                form.passwordVisibility.setContentDescription(getString(shown[0]?R.string.sb_hide_password:R.string.sb_show_password));
                p.setSelection(p.length());
            });
            add(frame.content,form.getRoot(),12);
            return p;
        }
        private void mode(int mode){if(!submitting){memory.putInt("mode",mode);refresh();}}
        @Override public void onDestroyView(){backToLogin=null;super.onDestroyView();}
    }
}
