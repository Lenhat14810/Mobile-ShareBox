package com.example.sharebox.ui;

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.Gravity;
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
        private final Runnable progress=new Runnable(){
            @Override public void run(){
                value=Math.min(100,value+(value<65?8:4));
                updateProgress();
                if(value<100)timer.postDelayed(this,80);
                else timer.postDelayed(finish,300);
            }
        };
        private final Runnable finish=()->host.reset(R.id.auth);
        public Splash(){super(R.layout.screen_splash);}
        @Override protected void render(){updateProgress();}
        private void updateProgress(){
            ProgressBar bar=requireView().findViewById(R.id.splash_progress);
            TextView hint=requireView().findViewById(R.id.splash_hint);
            bar.setProgress(value);
            hint.setText(getString(R.string.sb_loading_progress,value));
        }
        @Override public void onResume(){
            super.onResume();
            // The demo repository initializes synchronously when the Activity creates its ViewModel.
            if(value<100)timer.post(progress);
            else timer.postDelayed(finish,300);
        }
        @Override public void onPause(){
            timer.removeCallbacksAndMessages(null);
            super.onPause();
        }
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
            add(frame.content,submit,14);
            if(!registration)add(frame.content,feedback,10);
            if(mode==0){
                TextView or=meta("────────       hoặc       ────────");or.setGravity(Gravity.CENTER);add(frame.content,or,10);
                add(frame.content,button("G   Tiếp tục với Google",false,()->{
                    if(submitting)return;
                    submitting=true;feedback.setText("Đăng nhập mô phỏng thành công! Đang vào Trang chủ…");
                    vm.authenticate("","","",false,()->{if(frame!=null&&isAdded())host.reset(R.id.home);});
                }),12);
            }
            add(frame.content,link(mode==0?"Chưa có tài khoản? Đăng ký":"Đã có tài khoản? Đăng nhập",Gravity.CENTER,
                    ()->mode(mode==0?1:0)),registration?0:14);
            if(registration)add(frame.content,feedback,0);
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
            form.passwordVisibility.setOnClickListener(v->{
                boolean show=p.getTransformationMethod() instanceof PasswordTransformationMethod;
                int start=p.getSelectionStart(), end=p.getSelectionEnd();
                p.setTransformationMethod(show?HideReturnsTransformationMethod.getInstance():PasswordTransformationMethod.getInstance());
                form.passwordVisibility.setImageResource(show?R.drawable.sb_ic_eye_off:R.drawable.sb_ic_eye);
                form.passwordVisibility.setContentDescription(getString(show?R.string.sb_hide_password:R.string.sb_show_password));
                if(start>=0&&end>=0)p.setSelection(start,end);
            });
            add(frame.content,form.getRoot(),12);
            return p;
        }
        private void mode(int mode){if(!submitting){memory.putInt("mode",mode);refresh();}}
        @Override public void onDestroyView(){backToLogin=null;super.onDestroyView();}
    }
}
