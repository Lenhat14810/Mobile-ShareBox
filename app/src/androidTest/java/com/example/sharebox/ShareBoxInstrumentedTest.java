package com.example.sharebox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.NoMatchingViewException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.Espresso.pressBackUnconditionally;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static com.example.sharebox.data.Models.*;

@RunWith(AndroidJUnit4.class)
public final class ShareBoxInstrumentedTest {
    @Rule public final ActivityScenarioRule<MainActivity> activity=new ActivityScenarioRule<>(MainActivity.class);

    @Test public void authenticationAndFeatureDestinations() {
        shown("Chào mừng trở lại");
        tap("G   Tiếp tục với Google");
        shown("Sản phẩm nổi bật");
        tap("Khám phá");
        onView(withText("7 kết quả")).check(doesNotExist());
        tap("Tin nhắn");
        shown("Chat");
        tap("Cá nhân");
        shown("Sản phẩm của tôi");
        tap("Sản phẩm của tôi");
        shown("Tất cả 6");
        onView(allOf(withContentDescription("Quay lại"),isDisplayed())).perform(click());
        shown("Cài đặt");

        int[] routes={R.id.product,R.id.chat,R.id.ai_chat,R.id.tx_request,R.id.transactions,R.id.tx_detail,
                R.id.payment,R.id.finance,R.id.notifications,R.id.wishlist,R.id.edit_profile,R.id.my_products,
                R.id.create_product,R.id.settings,R.id.states};
        String[] headers={"Máy ảnh Canon EOS 200D kèm lens 18-55mm","Trần Quốc Lâm","ShareBox AI",
                "Yêu cầu thuê / mượn","Giao dịch của tôi","Chi tiết giao dịch","Số tiền cần thanh toán","Tài chính",
                "Thông báo","Yêu thích","Chỉnh sửa hồ sơ","Sản phẩm của tôi","Đăng tin mới","Cài đặt & hỗ trợ","Không thể kết nối"};
        for(int i=0;i<routes.length;i++){
            final int route=routes[i];
            activity.getScenario().onActivity(a->{
                Bundle args=new Bundle();
                args.putInt("id",route==R.id.product||route==R.id.chat||route==R.id.tx_request||
                        route==R.id.tx_detail||route==R.id.payment?1:0);
                a.open(route,args);
            });
            shown(headers[i]);
            activity.getScenario().onActivity(a->a.back());
        }
    }

    @Test public void requestSubmissionReusesChatAndListsPending() {
        activity.getScenario().onActivity(a->{
            a.reset(R.id.home);
            Bundle args=new Bundle();
            args.putInt("id",1);
            a.open(R.id.tx_request,args);
        });
        shown("Yêu cầu thuê / mượn");
        tap("Gửi yêu cầu");
        shown("Xem giao dịch của tôi");
        activity.getScenario().onActivity(a->{
            assertEquals(5,a.model.state.chats.size());
            Transaction created=a.model.state.transactions.get(0);
            assertEquals(1,created.productId);
            assertEquals(Stage.PENDING,created.stage());
            assertNotNull(a.model.state.chats.get(1).latest());
        });
        tap("Xem giao dịch của tôi");
        shown("Giao dịch của tôi");
    }

    @Test public void searchFinanceThemeAndRoleConfirmations() {
        activity.getScenario().onActivity(a->{
            a.reset(R.id.home);
        });
        tap("Khám phá");
        onView(withHint("Tìm theo tên hoặc từ khóa")).perform(replaceText("may anh"));
        onView(withText("1 kết quả")).check(doesNotExist());
        tap("Máy ảnh Canon EOS 200D");
        shown("Máy ảnh Canon EOS 200D kèm lens 18-55mm");
        pressBack();
        tap("Cá nhân");
        tap("Tài chính");
        shown("4.820.000đ");
        tap("Quý này");
        pressBack();
        tap("Cài đặt");
        onView(withContentDescription("Chế độ tối")).perform(click());
        shown("Cài đặt & hỗ trợ");
        activity.getScenario().onActivity(a->org.junit.Assert.assertTrue(a.model.state.dark));
        onView(withContentDescription("Chế độ tối")).perform(click());
        shown("Cài đặt & hỗ trợ");

        activity.getScenario().onActivity(a->{
            Bundle b=new Bundle();
            b.putInt("id",1);
            a.open(R.id.tx_detail,b);
        });
        confirm("Mô phỏng chủ đồ: Duyệt yêu cầu");
        tap("Xem thanh toán & đặt cọc");
        tap("Tiền mặt");
        confirm("Tôi đã thanh toán");
        stage(Stage.PAYMENT);
        confirm("Mô phỏng chủ đồ: Xác nhận đã nhận tiền");
        stage(Stage.HANDOVER);
        tap("Về chi tiết giao dịch");
        confirm("Tôi đã nhận đồ");
        stage(Stage.HANDOVER);
        confirm("Mô phỏng chủ đồ: Xác nhận đã giao đồ");
        stage(Stage.ACTIVE);
        confirm("Xác nhận đã trả đồ");
        stage(Stage.RETURN);
        tap("Mô phỏng chủ đồ: Xác nhận đã nhận lại");
        onView(withHint("0")).perform(replaceText("150000"));
        tap("Xác nhận");
        stage(Stage.COMPLETE);
        activity.getScenario().onActivity(a->assertEquals(850000L,a.model.state.transaction(1).refund()));
    }

    @Test public void splashOpensLoginWithoutAStartupBackStack() {
        onView(withText("Chạm để tiếp tục")).check(doesNotExist());
        shown("Chào mừng trở lại");
        shown("Đăng nhập");
        onView(withText("Khám phá đồ dùng")).check(doesNotExist());
        activity.getScenario().onActivity(a->{
            var host=(NavHostFragment)a.getSupportFragmentManager().findFragmentById(R.id.nav_host);
            assertNotNull(host);
            var nav=host.getNavController();
            assertEquals(R.id.auth,nav.getCurrentDestination().getId());
            assertNull(nav.getPreviousBackStackEntry());
            assertFalse(a.model.state.signedIn);
        });
        pressBackUnconditionally();
    }

    @Test public void registrationTogglesPasswordIconAndMask() {
        activity.getScenario().onActivity(a->a.reset(R.id.auth));
        tap("Chưa có tài khoản? Đăng ký");
        onView(withText("Đã có tài khoản? Đăng nhập")).perform(scrollTo()).check(matches(isDisplayed()));
        var firstField=allOf(withId(R.id.password_visibility),
                withParent(withParent(androidx.test.espresso.matcher.ViewMatchers.hasDescendant(withText("Mật khẩu")))));
        var eye=onView(firstField).perform(scrollTo());
        Bitmap[] icons=new Bitmap[2];
        eye.check((view,error)->{
            if(error!=null)throw error;
            icons[0]=iconImage((ImageButton)view);
            assertTrue(passwordInput((ImageButton)view).getTransformationMethod() instanceof PasswordTransformationMethod);
        });
        eye.perform(click()).check((view,error)->{
            if(error!=null)throw error;
            icons[1]=iconImage((ImageButton)view);
            assertFalse(passwordInput((ImageButton)view).getTransformationMethod() instanceof PasswordTransformationMethod);
            assertEquals("Ẩn mật khẩu",view.getContentDescription());
            assertFalse(icons[0].sameAs(icons[1]));
        });
        eye.perform(click()).check((view,error)->{
            if(error!=null)throw error;
            assertTrue(passwordInput((ImageButton)view).getTransformationMethod() instanceof PasswordTransformationMethod);
            assertEquals("Hiện mật khẩu",view.getContentDescription());
            assertTrue(icons[0].sameAs(iconImage((ImageButton)view)));
        });
    }

    @Test public void exploreSearchAndFavoriteReflectCurrentState() {
        startAtHome();
        tap("Khám phá");
        var search=onView(withHint("Tìm theo tên hoặc từ khóa"));
        int[] width={0};
        search.check(matches(isCompletelyDisplayed())).check((view,error)->width[0]=view.getWidth());
        tap("Điện tử");
        search.perform(replaceText("may anh"));
        shown("1 kết quả");
        search.check((view,error)->assertEquals(width[0],view.getWidth()));
        var heart=onView(allOf(withId(R.id.favorite),isDisplayed()));
        Bitmap[] outlined={null};
        heart.check(matches(isSelected())).perform(click()).check(matches(not(isSelected())));
        heart.check((view,error)->outlined[0]=iconImage((ImageButton)view));
        activity.getScenario().onActivity(a->assertFalse(a.model.state.favorites.contains(1)));
        heart.perform(click()).check(matches(isSelected())).check((view,error)->
                assertFalse(outlined[0].sameAs(iconImage((ImageButton)view))));
        activity.getScenario().onActivity(a->assertTrue(a.model.state.favorites.contains(1)));
        heart.perform(click()).check(matches(not(isSelected()))).check((view,error)->
                assertTrue(outlined[0].sameAs(iconImage((ImageButton)view))));
        onView(withContentDescription("Xóa tìm kiếm")).perform(click());
        shown("3 kết quả");
        search.check((view,error)->assertEquals(width[0],view.getWidth()));
        onView(withContentDescription("Giá thuê mỗi ngày")).perform(click());
        tap("500.000 – dưới 1.000.000 VND");
        shown("0 kết quả");
        onView(withText("Điện tử")).check(matches(isSelected()));
        onView(allOf(withText("Miễn phí"),isClickable())).perform(click());
        shown("0 kết quả");
        tap("Xóa bộ lọc");
        onView(withText("7 kết quả")).check(doesNotExist());
        onView(withText("Tất cả")).check(matches(isSelected()));
    }

    @Test public void homeControlsAndProfileAboutOpenTheirOwnContent() {
        startAtHome();
        onView(withContentDescription("ShareBox AI")).check(doesNotExist());
        onView(withContentDescription("Chế độ tối")).perform(click());
        onView(withContentDescription("Chế độ sáng")).check(matches(isDisplayed())).perform(click());
        onView(withContentDescription("Chế độ tối")).check(matches(isDisplayed()));
        onView(withContentDescription("Thông báo")).perform(click());
        shown("Thông báo");
        pressBack();
        tap("▣  Tài chính  ›");
        shown("Tài chính");
        pressBack();
        tap("Cá nhân");
        onView(withText("Về ShareBox")).perform(scrollTo(),click());
        shown("ShareBox 1.0.0\nChia sẻ tiện ích – Kết nối cộng đồng.");
        tap("Đóng");
        shown("Cài đặt");
        onView(withText("Cài đặt & hỗ trợ")).check(doesNotExist());
    }

    private void startAtHome(){
        activity.getScenario().onActivity(a->{
            a.reset(R.id.home);
        });
    }

    @Test public void bottomTabsAndInboxUnreadWorkInBothThemes() {
        startAtHome();
        verifyTabsAndInbox();
        tap("Trang chủ");
        onView(withContentDescription("Chế độ tối")).perform(click());
        onView(withContentDescription("Chế độ sáng")).check(matches(isDisplayed()));
        verifyTabsAndInbox();
        tap("Trang chủ");
        onView(withContentDescription("Chế độ sáng")).perform(click());
        onView(withContentDescription("Chế độ tối")).check(matches(isDisplayed()));
    }

    @Test public void transactionLabelsAndAllFilterRemainUsable(){
        startAtHome();
        activity.getScenario().onActivity(a->a.open(R.id.transactions,null));
        shown("Giao dịch của tôi");
        onView(allOf(withText("Bạn thuê"),hasSibling(withText("Chờ xác nhận")),isDisplayed()))
                .check((view,error)->{
                    if(error!=null)throw error;
                    assertNull(view.getBackground());
                    assertNull(((LinearLayout)view.getParent()).getChildAt(1).getBackground());
                });
        onView(allOf(withText(startsWith("Tất cả ")),isDisplayed()))
                .check(matches(isSelected())).perform(click());
    }

    @Test public void listingDraftIsDiscardedWhenSwitchingTabs(){
        startAtHome();
        tap("Tin nhắn");
        var plus=onView(withId(R.id.nav_create));
        plus.perform(click());
        verifySelectedTab(R.id.nav_create);
        shown("Đăng tin mới");
        pressBack();
        shown("Chat");
        verifySelectedTab(R.id.nav_chat);

        plus.perform(click());
        var name=onView(withHint("Ví dụ: Máy ảnh Canon EOS 200D"));
        name.perform(replaceText("Bản nháp"),closeSoftKeyboard());
        pressBack();
        shown("Bỏ thay đổi chưa lưu?");
        tap("Tiếp tục chỉnh sửa");
        name.check(matches(withText("Bản nháp")));
        onView(withContentDescription("Quay lại")).perform(click());
        shown("Bỏ thay đổi chưa lưu?");
        tap("Tiếp tục chỉnh sửa");
        tap("Trang chủ");
        shown("Bỏ thay đổi chưa lưu?");
        tap("Tiếp tục chỉnh sửa");
        name.check(matches(withText("Bản nháp")));
        tap("Trang chủ");
        tap("Bỏ thay đổi");
        shown("Sản phẩm nổi bật");
        tap("Tin nhắn");
        shown("Chat");
        onView(withHint("Ví dụ: Máy ảnh Canon EOS 200D")).check(doesNotExist());
        plus.perform(click());
        name.check(matches(withText("")));
        verifySelectedTab(R.id.nav_create);
        onView(withText("Như mới")).check(doesNotExist());
        name.perform(replaceText("Máy thử điều kiện"));
        onView(allOf(withText("Mới"),isClickable())).perform(scrollTo()).check(matches(isDisplayed()));
        onView(allOf(withText("Đã dùng"),isClickable())).perform(scrollTo(),click());
        activity.getScenario().onActivity(a->{
            var root=a.findViewById(R.id.content);
            var controls=new java.util.ArrayList<View>();
            root.findViewsWithText(controls,"Cho mượn miễn phí",View.FIND_VIEWS_WITH_TEXT);
            assertEquals(1,controls.size());
            controls.get(0).performClick();
        });
        onView(allOf(withText("Đăng tin"),withParent(withId(R.id.footer)))).perform(click());
        shown("Đăng tin thành công! Tin của bạn đang hiển thị trong Khám phá.");
        activity.getScenario().onActivity(a->assertEquals("Đã dùng",a.model.state.products.get(0).condition));
    }

    @Test public void homeAiShortcutIsOptInAndOpensTheAssistant(){
        activity.getScenario().onActivity(a->a.setHomeAiShortcut(false));
        try{
            startAtHome();
            onView(withText("Mở ShareBox AI")).check(doesNotExist());
            tap("Cá nhân");
            tap("Cài đặt");
            onView(withContentDescription("Lối tắt ShareBox AI")).perform(click());
            tap("Trang chủ");
            tap("Mở ShareBox AI");
            shown("ShareBox AI");
        }finally{
            activity.getScenario().onActivity(a->a.setHomeAiShortcut(false));
        }
    }

    @Test public void listingValidationRevealsTheFirstOffscreenError(){
        startAtHome();
        onView(withId(R.id.nav_create)).perform(click());
        onView(withHint("Ví dụ: Máy ảnh Canon EOS 200D")).perform(replaceText("Tên hợp lệ"),closeSoftKeyboard());
        onView(allOf(withText("Đăng tin"),withParent(withId(R.id.footer)))).perform(click());
        shown("Nhập giá thuê mỗi ngày");
        onView(withHint("250000")).check(matches(isDisplayed())).check((view,error)->{
            if(error!=null)throw error;
            assertTrue(view.hasFocus());
        });
    }

    @Test public void auditFixesWorkInLightAndDarkThemes(){
        startAtHome();
        verifyAuditFixes();
        onView(withContentDescription("Chế độ tối")).perform(click());
        onView(withContentDescription("Chế độ sáng")).check(matches(isDisplayed()));
        verifyAuditFixes();
        onView(withContentDescription("Chế độ sáng")).perform(click());
        onView(withContentDescription("Chế độ tối")).check(matches(isDisplayed()));
    }

    private void verifyAuditFixes(){
        tap("Khám phá");
        onView(withContentDescription("Mở ShareBox AI")).check(doesNotExist());
        var search=onView(withHint("Tìm theo tên hoặc từ khóa"));
        search.perform(replaceText("canon"),closeSoftKeyboard());
        onView(withContentDescription("Dạng danh sách")).perform(click());
        var sort=onView(withContentDescription(startsWith("Sắp xếp · ")));
        CharSequence[] order={null};
        sort.perform(click()).check((view,error)->order[0]=view.getContentDescription());
        onView(withContentDescription("Giá thuê mỗi ngày")).perform(click());
        tap("Dưới 500.000 VND");
        shown("1 kết quả");
        shown("Xóa bộ lọc");
        onView(withContentDescription("Giá thuê mỗi ngày · Dưới 500.000 VND")).perform(click());
        tap("Trên 5.000.000 VND");
        shown("0 kết quả");
        onView(withContentDescription("Tình trạng")).check(matches(isCompletelyDisplayed()));
        tap("Xóa bộ lọc");
        search.check(matches(withText("canon")));
        onView(withContentDescription("Dạng danh sách")).check(matches(isSelected()));
        sort.check((view,error)->assertEquals(order[0],view.getContentDescription()));
        shown("Máy ảnh Canon EOS 200D");

        onView(withId(R.id.nav_create)).perform(click());
        var name=onView(withHint("Ví dụ: Máy ảnh Canon EOS 200D"));
        name.perform(replaceText("Kiểm tra bản nháp"),closeSoftKeyboard());
        tap("Trang chủ");
        shown("Bỏ thay đổi chưa lưu?");
        tap("Tiếp tục chỉnh sửa");
        name.check(matches(withText("Kiểm tra bản nháp")));
        tap("Trang chủ");
        tap("Bỏ thay đổi");
        shown("Sản phẩm nổi bật");
    }

    @Test public void locationAndConditionPickersCanBeCombined(){
        startAtHome();
        tap("Khám phá");
        onView(withContentDescription("Địa điểm")).perform(click());
        tap("TP. Hồ Chí Minh");
        shown("7 kết quả");

        onView(withContentDescription("Tình trạng")).perform(click());
        onView(withText("Như mới")).check(doesNotExist());
        tap("Mới");
        shown("0 kết quả");
        onView(withContentDescription("Tình trạng · Mới")).perform(click());
        tap("Đã dùng");
        shown("7 kết quả");
        onView(withContentDescription("Địa điểm · TP. Hồ Chí Minh")).check(matches(isSelected()));
        onView(withContentDescription("Địa điểm · TP. Hồ Chí Minh")).perform(click());
        tap("Hà Nội");
        shown("0 kết quả");
    }

    private void verifyTabsAndInbox(){
        verifySelectedTab(R.id.nav_home);
        verifyHomeHeader();
        tap("Khám phá");
        verifySelectedTab(R.id.nav_explore);
        verifyExploreControls();
        var sort=onView(withContentDescription(startsWith("Sắp xếp · ")));
        CharSequence[] sortBefore={null};
        sort.check(matches(isCompletelyDisplayed()))
                .check((view,error)->sortBefore[0]=view.getContentDescription()).perform(click());
        sort.check((view,error)->assertFalse(sortBefore[0].equals(view.getContentDescription())));
        onView(withContentDescription("Dạng danh sách")).perform(click()).check(matches(isSelected()));
        tap("Tin nhắn");
        verifySelectedTab(R.id.nav_chat);
        onView(withHint("Tìm cuộc trò chuyện")).perform(replaceText("tran quoc lam"),closeSoftKeyboard());
        shown("Trần Quốc Lâm");
        var menu=onView(withContentDescription("Tùy chọn cho Trần Quốc Lâm"));
        long before=unreadForCamera();
        menu.check(matches(isCompletelyDisplayed())).check((view,error)->{
            if(error!=null)throw error;
            assertEquals(view.getWidth(),view.getHeight());
            if(before>0){
                var group=(LinearLayout)view.getParent();
                var badge=(TextView)group.getChildAt(group.getChildCount()-2);
                assertTrue(view.getLeft()>badge.getRight());
                assertEquals(badge.getWidth(),badge.getHeight());
                assertEquals(Color.WHITE,badge.getCurrentTextColor());
                assertEquals(0xffb72a47,((GradientDrawable)badge.getBackground()).getColor().getDefaultColor());
            }
        }).perform(click());
        tap(before>0?"Đánh dấu đã đọc":"Đánh dấu chưa đọc");
        assertEquals(before>0?0:1,unreadForCamera());
        var badge=onView(withContentDescription("1 tin nhắn chưa đọc"));
        if(before>0)badge.check(doesNotExist());
        else badge.check(matches(isDisplayed()));
        shown("✧   ShareBox AI");
        tap("Cá nhân");
        verifySelectedTab(R.id.nav_profile);
    }

    private void verifyExploreControls(){
        View[] groups=new View[4];
        onView(withText("Tất cả")).check(matches(isSelected())).check((view,error)->{
            if(error!=null)throw error;
            var label=(TextView)view;
            assertEquals(0,label.getPaintFlags()&Paint.UNDERLINE_TEXT_FLAG);
            var fill=(GradientDrawable)((RippleDrawable)label.getBackground()).getDrawable(0).getCurrent();
            assertTrue(ColorUtils.calculateContrast(label.getCurrentTextColor(),fill.getColor().getDefaultColor())>=4.5);
            groups[0]=(View)label.getParent();
        });
        onView(allOf(withText("Miễn phí"),isClickable())).check((view,error)->{
            if(error!=null)throw error;
            assertNotNull(view.getBackground());
            assertFalse(view.getParent()==groups[0]);
        });
        for(String label:new String[]{"Giá thuê mỗi ngày","Địa điểm","Tình trạng"}){
            onView(withContentDescription(label)).check((view,error)->{
                if(error!=null)throw error;
                assertNotNull(view.getBackground());
                assertFalse(view.getParent()==groups[0]);
                assertNull(((View)view.getParent()).getBackground());
            });
        }
        View[] track={null};
        onView(withContentDescription("Dạng lưới")).check((view,error)->track[0]=(View)view.getParent());
        onView(withContentDescription("Dạng danh sách")).check((view,error)->{
            assertTrue(view.getParent()==track[0]);
            assertNotNull(track[0].getBackground());
            assertTrue(((View)track[0].getParent()).getElevation()>0);
        });
        onView(withContentDescription(startsWith("Sắp xếp · "))).check((view,error)->{
            var sort=(TextView)view;
            assertNotNull(((RippleDrawable)sort.getBackground()).getDrawable(0).getCurrent());
            assertEquals("Mới nhất".contentEquals(sort.getText()),sort.isSelected());
        });
    }

    private void verifyHomeHeader(){
        onView(withContentDescription("Thông báo")).check(matches(isCompletelyDisplayed())).check((view,error)->{
            if(error!=null)throw error;
            var notifications=(android.widget.FrameLayout)view.getParent();
            var identity=(ConstraintLayout)notifications.getParent();
            View avatar=identity.getChildAt(0);
            View theme=identity.getChildAt(2);
            var username=(TextView)identity.getChildAt(3);
            var finance=(TextView)identity.getChildAt(4);
            float density=view.getResources().getDisplayMetrics().density;
            assertTrue(notifications.getLeft()-theme.getRight()>=Math.round(8*density));
            assertTrue(view.getWidth()>=Math.round(48*density));
            assertTrue(Math.abs((username.getTop()+username.getBottom())-(avatar.getTop()+avatar.getBottom()))<=2);
            assertTrue(username.getLeft()>=avatar.getRight()&&username.getRight()<=theme.getLeft());
            assertEquals(64,Math.round(avatar.getWidth()/density));
            assertEquals(avatar.getWidth(),avatar.getHeight());
            assertTrue(avatar.getClipToOutline());
            Outline outline=new Outline();
            avatar.getOutlineProvider().getOutline(avatar,outline);
            assertEquals(avatar.getWidth()/2f,outline.getRadius(),1f);
            assertEquals(username.getLeft(),finance.getLeft());
            assertEquals(username.getBottom(),finance.getTop());
            assertEquals(1,username.getMaxLines());
            assertEquals(android.text.TextUtils.TruncateAt.END,username.getEllipsize());
            assertEquals(30,Math.round((view.getWidth()-view.getPaddingLeft()-view.getPaddingRight())/density));
            assertEquals(30,Math.round((theme.getWidth()-theme.getPaddingLeft()-theme.getPaddingRight())/density));
            var badge=(TextView)notifications.getChildAt(1);
            int surface=((GradientDrawable)badge.getBackground()).getColor().getDefaultColor();
            assertTrue(ColorUtils.calculateContrast(badge.getCurrentTextColor(),surface)>=4.5);
        });
    }

    private void verifySelectedTab(int selected){
        activity.getScenario().onActivity(a->{
            for(int id:new int[]{R.id.nav_home,R.id.nav_explore,R.id.nav_create,R.id.nav_chat,R.id.nav_profile}){
                View tab=a.findViewById(id);
                assertEquals(id==selected,tab.isSelected());
                assertEquals(id==selected,tab.getBackground().getCurrent() instanceof InsetDrawable);
            }
        });
    }

    private long unreadForCamera(){
        long[] unread={0};
        activity.getScenario().onActivity(a->unread[0]=a.model.state.chats.get(1).unread);
        return unread[0];
    }

    private static EditText passwordInput(ImageButton toggle){
        return ((LinearLayout)toggle.getParent()).findViewById(R.id.password_input);
    }

    private static Bitmap iconImage(ImageButton button){
        Bitmap image=Bitmap.createBitmap(button.getWidth(),button.getHeight(),Bitmap.Config.ARGB_8888);
        button.getDrawable().draw(new Canvas(image));
        return image;
    }

    private void stage(Stage expected){
        activity.getScenario().onActivity(a->assertEquals(expected,a.model.state.transaction(1).stage()));
    }
    private static void confirm(String label){
        tap(label);
        tap("Xác nhận");
    }

    private static void tap(String label) {
        onView(allOf(withText(label),isDisplayed())).perform(click());
    }
    private static void shown(String label) {
        long until=android.os.SystemClock.uptimeMillis()+12000;
        NoMatchingViewException last=null;
        do{
            try{
                onView(allOf(withText(label),isDisplayed())).check(matches(isDisplayed()));
                return;
            }catch(NoMatchingViewException wait){last=wait;android.os.SystemClock.sleep(150);}
        }while(android.os.SystemClock.uptimeMillis()<until);
        throw last;
    }
}
