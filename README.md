# ShareBox Android — Java + XML

ShareBox là bản mô phỏng giao diện Android cho thuê và mượn đồ. Dự án tiếp tục từ bản ShareBox hiện có, với cùng mã ứng dụng `com.example.sharebox`, cấu hình SDK và bộ minh họa sản phẩm. Bố cục bám theo **ShareBox – Android UI UX-selection.pdf**. Điều hướng và nghiệp vụ bám theo **ShareBox_Luong_xu_ly_man_hinh.pdf**.

## Mở và chạy

1. Giải nén `ShareBox-Java-Frontend.zip`, mở thư mục `Sharebox` bằng Android Studio.
2. Cài SDK Platform 37.0 và Android SDK Build-Tools 36.0.0. Dùng JDK 25 cho Gradle. Chạy trên thiết bị hoặc trình mô phỏng từ API 26.
3. Chạy ứng dụng hoặc dùng Gradle wrapper. File APK gỡ lỗi nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

```bat
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest
gradlew.bat connectedDebugAndroidTest
```

Trên macOS/Linux, dùng `./gradlew` với các tác vụ tương ứng. Kiểm thử thiết bị cần một thiết bị hoặc trình mô phỏng đã khởi động. Lần đồng bộ Gradle đầu tiên cần truy cập các kho thư viện.

Có thể đăng nhập bằng một email hợp lệ và mật khẩu mẫu tối thiểu 6 ký tự, hoặc bấm **Tiếp tục với Google** để dùng tài khoản mẫu. Kho dữ liệu cục bộ giữ thay đổi trong phiên chạy. Không cần cấu hình máy chủ. Mốc ngày và dữ liệu tài chính mẫu dùng tháng 12/2026 theo tài liệu.

## Màn hình

Mỗi hàng tương ứng một đích trong `res/navigation/sharebox_graph.xml`, một layout XML và một Fragment Java.

| # | Màn hình | Layout | Tương tác mẫu |
| --- | --- | --- | --- |
| 1 | Splash | `screen_splash` | Tiến trình, tự chuyển hoặc chạm để tiếp tục |
| 2 | Giới thiệu | `screen_onboarding` | Vuốt ba trang, chọn chấm trang, bỏ qua |
| 3 | Xác thực | `screen_auth` | Đăng nhập, đăng ký, quên mật khẩu và kiểm tra dữ liệu nhập |
| 4 | Trang chủ | `screen_home` | Gợi ý tìm kiếm, tìm tại chỗ, banner, nhóm sản phẩm |
| 5 | Khám phá | `screen_explore` | Tìm không dấu, bộ lọc, sắp xếp, lưới/danh sách |
| 6 | Chi tiết sản phẩm | `screen_product` | Ảnh, yêu thích, lịch trống, báo cáo, chat, yêu cầu |
| 7 | Tin nhắn | `screen_chats` | Tìm, đọc/chưa đọc, tắt tiếng, chặn, lưu trữ và xóa |
| 8 | Chat | `screen_chat` | Nhắn tin, xem và gửi ảnh/QR, tạo yêu cầu |
| 9 | ShareBox AI | `screen_ai_chat` | Câu hỏi nhanh, nhập câu hỏi, phản hồi mô phỏng, gợi ý sản phẩm |
| 10 | Yêu cầu thuê/mượn | `screen_tx_request` | 1–14 ngày, cọc riêng, gửi từ cuộc chat có sẵn |
| 11 | Giao dịch của tôi | `screen_transactions` | Tab trạng thái, vai trò, chi tiết và tài chính |
| 12 | Chi tiết giao dịch | `screen_tx_detail` | Chấp nhận, bàn giao/trả đồ hai phía, hủy, tranh chấp, đánh giá |
| 13 | Thanh toán & đặt cọc | `screen_payment` | Tiền mặt/chuyển khoản, QR, chứng từ và xác nhận hai phía |
| 14 | Tài chính | `screen_finance` | Tháng/quý/năm, biểu đồ sáu tháng, thu/chi/cọc |
| 15 | Thông báo | `screen_notifications` | Lọc, đọc một/tất cả, mở màn hình liên quan |
| 16 | Yêu thích | `screen_wishlist` | Bỏ lưu, chat, mở chi tiết |
| 17 | Cá nhân | `screen_profile` | Hồ sơ, menu tài khoản, đăng xuất |
| 18 | Chỉnh sửa hồ sơ | `screen_edit_profile` | Ảnh đại diện, email chỉ đọc, giới hạn giới thiệu 150 ký tự |
| 19 | Sản phẩm của tôi | `screen_my_products` | Lọc tin hoạt động/ẩn/gỡ, sửa, ẩn, hiện và gỡ |
| 20 | Đăng tin | `screen_create_product` | Tạo/sửa, tối đa tám ảnh, chọn bìa, giá và cọc |
| 21 | Cài đặt & hỗ trợ | `screen_settings` | Giao diện sáng/tối, tài khoản, thông báo, riêng tư, trợ giúp |
| 22 | Trạng thái hệ thống | `screen_states` | Khung xương, không có kết quả, lỗi kết nối và thử lại |

## Tệp và cấu trúc

Các tệp Kotlin được thay gồm `MainActivity.kt`, `Models.kt`, `ShareBoxRepository.kt`, `Design.kt`, `ShareBoxApp.kt`, `ShareBoxViewModel.kt`, `AccountScreens.kt`, `AuthScreens.kt`, `CatalogScreens.kt`, `ChatScreens.kt`, `DashboardScreens.kt`, `ListingScreens.kt`, `TransactionScreens.kt`, `ShareBoxFlowTest.kt` và `ExampleInstrumentedTest.kt`.

Các tệp Java mới:

- `MainActivity.java` quản lý NavHost, tab dưới, bàn phím, chủ đề và nút AI kéo thả.
- `data/Models.java`, `data/ShareBoxRepository.java`, `data/MockShareBoxRepository.java` chứa mô hình nghiệp vụ, hợp đồng kho dữ liệu và dữ liệu mẫu.
- `ui/Page.java` chứa hành vi khung màn hình, ViewBinding, adapter RecyclerView và tải ảnh cục bộ bằng trình giải mã ảnh Android.
- `ui/ShareBoxViewModel.java` giữ trạng thái phiên, phát cập nhật qua LiveData và chạy mô phỏng bất đồng bộ với Java ExecutorService.
- `ui/AuthPages.java`, `ui/CatalogPages.java`, `ui/ChatPages.java`, `ui/TransactionPages.java`, `ui/ProfilePages.java` chứa các Fragment theo nhóm chức năng.
- `ShareBoxFlowTest.java` kiểm tra chuyển trạng thái theo vai trò, cọc, tranh chấp, hủy và lịch. `ShareBoxInstrumentedTest.java` kiểm tra luồng mở ứng dụng, cả 22 đích điều hướng, yêu cầu dùng lại đoạn chat, tìm kiếm, tài chính, chủ đề và xác nhận hai bên qua giao diện.

Các XML dùng chung gồm `activity_main`, `screen_frame`, `card_product`, `component_field`, `component_panel`, `component_composer`, `component_gradient_banner` `component_system_error`, `component_auth_header` và `component_password`. Bộ màu sáng/tối, kích thước, vector, hình minh họa, selector và văn bản XML nằm trong `res`. Fragment gắn dữ liệu và tạo các phần lặp theo trạng thái.

## Giao dịch mẫu

Mở máy ảnh Canon, chat với chủ đồ và tạo yêu cầu. Màn xác nhận gửi cung cấp nút **Xem giao dịch của tôi**. Trong chi tiết có các nút gắn nhãn **Mô phỏng** cho phía còn lại. Thanh toán, giao đồ và trả đồ lưu xác nhận của từng bên. Tranh chấp tạm dừng các bước đang dở cho tới khi hủy báo. Phí thuê và cọc được tách riêng trong bảng tiền và sổ tài chính. Ảnh và QR được chọn từ thiết bị. ShareBox không xác minh chuyển tiền.

Bạn có thể mở các trạng thái mẫu tại **Cá nhân → Cài đặt → Trạng thái danh sách**.
