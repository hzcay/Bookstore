console.log("=== DEBT REPORT JS LOADED ===");

document.addEventListener("DOMContentLoaded", function () {
     console.log("Initializing debt report...");

     if (typeof bootstrap === "undefined") {
          console.error("Bootstrap not found");
          return;
     }

     console.log("Bootstrap version:", bootstrap.Modal.VERSION);

     var payModal = new bootstrap.Modal(document.getElementById("payModal"), {
          backdrop: "static",
          keyboard: false,
     });

     var buttons = document.querySelectorAll(".btn-pay");
     console.log("Found pay buttons:", buttons.length);

     buttons.forEach(function (btn, index) {
          btn.addEventListener("click", function (e) {
               e.preventDefault();
               console.log("Pay button", index, "clicked!");

               var supplierId = this.getAttribute("data-supplier-id");
               var supplierName = this.getAttribute("data-supplier-name");
               var debt = this.getAttribute("data-debt");

               console.log("Supplier data:", {
                    id: supplierId,
                    name: supplierName,
                    debt: debt,
               });

               if (!supplierId || !debt) {
                    alert("Lỗi: Không tìm thấy thông tin nhà cung cấp");
                    return;
               }

               document.getElementById("supplierIdInput").value = supplierId;
               document.getElementById("supplierNameInput").value = supplierName || "N/A";
               document.getElementById("currentDebtInput").value =
                    new Intl.NumberFormat("vi-VN").format(debt) + " ₫";

               console.log("Opening modal...");
               payModal.show();
          });
     });

     console.log("=== INITIALIZATION COMPLETE ===");
});
