// Debug: Kiểm tra file JS đã load
console.log("✅ receipts-export.js loaded successfully!");

// Hàm set preset thời gian
function setDatePreset(preset) {
     console.log("setDatePreset called with:", preset);
     const now = new Date();
     const fromInput = document.getElementById("filterFrom");
     const toInput = document.getElementById("filterTo");

     if (!fromInput || !toInput) {
          console.error("❌ Input elements not found!");
          return;
     }

     let fromDate, toDate;

     switch (preset) {
          case "today":
               fromDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0);
               toDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59);
               break;
          case "thisWeek":
               const dayOfWeek = now.getDay();
               const monday = new Date(now);
               monday.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
               fromDate = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate(), 0, 0);
               toDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59);
               break;
          case "thisMonth":
               fromDate = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0);
               toDate = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59);
               break;
          case "thisYear":
               fromDate = new Date(now.getFullYear(), 0, 1, 0, 0);
               toDate = new Date(now.getFullYear(), 11, 31, 23, 59);
               break;
     }

     fromInput.value = formatDateTimeLocal(fromDate);
     toInput.value = formatDateTimeLocal(toDate);
     console.log("✅ Date preset applied:", fromInput.value, "-", toInput.value);
}

// Hàm xóa filter
function clearDateFilter() {
     console.log("clearDateFilter called");
     document.getElementById("filterFrom").value = "";
     document.getElementById("filterTo").value = "";
}

// Hàm format datetime cho input type="datetime-local"
function formatDateTimeLocal(date) {
     const year = date.getFullYear();
     const month = String(date.getMonth() + 1).padStart(2, "0");
     const day = String(date.getDate()).padStart(2, "0");
     const hours = String(date.getHours()).padStart(2, "0");
     const minutes = String(date.getMinutes()).padStart(2, "0");
     return `${year}-${month}-${day}T${hours}:${minutes}`;
}

// Hàm xuất Excel với filter
function exportExcelWithFilter() {
     console.log("exportExcelWithFilter called");
     const fromInput = document.getElementById("filterFrom").value;
     const toInput = document.getElementById("filterTo").value;

     console.log("From:", fromInput, "To:", toInput);

     let url = "/api/warehouse/receipts/export?";
     const params = [];

     if (fromInput) {
          // Convert datetime-local format to ISO string
          const fromDate = new Date(fromInput);
          params.push("from=" + encodeURIComponent(fromDate.toISOString()));
     }

     if (toInput) {
          const toDate = new Date(toInput);
          params.push("to=" + encodeURIComponent(toDate.toISOString()));
     }

     url += params.join("&");
     console.log("📥 Downloading Excel from:", url);

     // Mở link download
     window.location.href = url;
}
