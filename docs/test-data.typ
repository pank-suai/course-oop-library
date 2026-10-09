#let test-count = 67
#let core-test-count = 17
#let app-test-count = 50
#let test-rows = (
  (name: "DomainRewriteTest", count: 4, description: "Проверки библиотечных объектов и правил"),
  (name: "ControlScenarioTest", count: 1, description: "Связанный демонстрационный набор без повторов"),
  (name: "RoomAdministrationTest", count: 2, description: "Загрузка и транзакционная очистка"),
  (name: "ManyToManyTest", count: 1, description: "Одно издание и разные читатели"),
  (name: "ReviewRegressionTest", count: 5, description: "Остаток, лимит выдач, состояние и дата возврата"),
  (name: "RoomInvariantTest", count: 2, description: "Согласованность сохранения и повторные операции"),
  (name: "RoomPersistenceTest", count: 2, description: "Запись, связи и повторное открытие базы"),
  (name: "CatalogInteractionTest", count: 12, description: "Формы, поиск, выдача, возврат, ошибки и адаптивное окно"),
  (name: "LibraryDesignTest", count: 3, description: "Параметры темы, контраст и навигация"),
  (name: "LoanDateFormatterTest", count: 1, description: "Понятное представление даты выдачи"),
  (name: "LoansSelectionTest", count: 3, description: "Очистка устаревшего выбора издания"),
  (name: "PublicationSaveInteractionTest", count: 2, description: "Форма издания при успехе и ошибке"),
  (name: "ReaderSaveInteractionTest", count: 1, description: "Форма читателя при ошибке и повторном нажатии"),
  (name: "TestDataInteractionTest", count: 5, description: "Учебные кнопки, встроенные обложки и отсутствие повторов"),
  (name: "TestDataViewModelTest", count: 3, description: "Состояния добавления учебных записей"),
  (name: "ViewModelLifecycleAndErrorTest", count: 15, description: "Ошибки, отмена задач, просрочка и завершение работы"),
  (name: "ViewModelOperationsTest", count: 4, description: "Операции выдачи, возврата и управления"),
  (name: "ViewModelValidationTest", count: 1, description: "Проверка обязательных значений формы"),
)
