const form = document.getElementById('employeeForm');
const tableBody = document.getElementById('employeeTable');
const editIndexInput = document.getElementById('editIndex');

let employees = JSON.parse(localStorage.getItem('employees')) || [];

function saveToLocalStorage() {
    localStorage.setItem('employees', JSON.stringify(employees));
}

form.addEventListener('submit', (e) => {
    e.preventDefault();
    const name = document.getElementById('name').value;
    const dept = document.getElementById('dept').value;
    const index = parseInt(editIndexInput.value);

    if (index === -1) {
        employees.push({ name, dept });
    } else {
        employees[index] = { name, dept };
        editIndexInput.value = -1;
    }

    saveToLocalStorage();
    form.reset();
    renderTable();
});

function renderTable() {
    tableBody.innerHTML = '';
    employees.forEach((emp, index) => {
        tableBody.innerHTML += `
            <tr>
                <td>${emp.name}</td>
                <td>${emp.dept}</td>
                <td>
                    <button class="btn btn-sm btn-warning" onclick="editEmployee(${index})">Editar</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteEmployee(${index})">Eliminar</button>
                </td>
            </tr>
        `;
    });
}

function deleteEmployee(index) {
    employees.splice(index, 1);
    saveToLocalStorage();
    renderTable();
}

function editEmployee(index) {
    const emp = employees[index];
    document.getElementById('name').value = emp.name;
    document.getElementById('dept').value = emp.dept;
    editIndexInput.value = index;
}

renderTable();
