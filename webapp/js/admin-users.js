// 회원 상태 변경 드롭다운 전체 선택
const statusSelects = document.querySelectorAll(".user-status-select");

statusSelects.forEach((select) => {

  select.addEventListener("change", function () {

    // '관리 선택'을 선택한 경우 아무 작업도 하지 않음
    if (this.value === "") {
      return;
    }

    // 현재 선택한 회원의 행
    const userRow = this.closest("tr");

    // 회원번호
    const userId = userRow.dataset.userId;

    // 관리자가 선택한 새로운 상태
    const newStatus = this.value;

    // 현재 상태가 표시되는 칸
    const statusCell = userRow.querySelector(".user-status");

    /*
      ==========================================
      추후 백엔드 연동 영역
      ==========================================

      백엔드 담당자가 이 위치에서

      userId
      newStatus

      값을 서버로 전달하면 됨.

      예)

      userId = 2
      newStatus = "ACTIVE"

      서버에서 USERS 테이블의 STATUS를
      ACTIVE 또는 SUSPENDED로 변경한 뒤

      변경 성공 응답을 받으면
      아래 화면 상태 변경 코드를 실행하면 됨.
    */


    // 현재는 프론트 화면 확인을 위한 임시 처리
    statusCell.textContent = newStatus;

    // 관리 메뉴를 다시 '관리 선택'으로 초기화
    this.value = "";

    // 개발 확인용
    console.log("회원번호 :", userId);
    console.log("변경 상태 :", newStatus);

  });

});